/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <cassert>
#include <cstdlib>
#include "synthesizer.h"
#include "trace.h"
#include "android_log.h"

#define DEFAULT_SINE_WAVE_FREQUENCY 440.0

#define BLEND_ALWAYS        1
#define BLEND_TRANSITION    2
#define BLEND_NEVER         3

Synthesizer::Synthesizer(
        int num_audio_channels,
        int frame_rate) :
        num_audio_channels_(num_audio_channels),
        frame_rate_(frame_rate) {

    soundDataArray = nullptr;

    // set defaults
    setFrequency(DEFAULT_SINE_WAVE_FREQUENCY);
    setVolume(0);
    setPreferences(0, 0, BLEND_ALWAYS);

}

int Synthesizer::render(int num_samples, int16_t *audio_buffer) {

    Trace::beginSection("Synthesizer::render");

    assert(audio_buffer != nullptr);

    // Do some floating point operations to simulate the load required to produce complex
    // synthesizer voices
    float x = 0;
    for (int i = 1; i <= work_cycles_; i++) {
        float y = 1 / i;
        float z = 2 / i;
        x = x / (y * z);
    }

    static int blend_mode = BLEND_NEVER;
    static int sound1_i = -1;
    static int sound2_i = -1;
    static float sound1_volume = 0;
    static float sound2_volume = 0;
    static float sound1_index_increment = 0;
    static float sound2_index_increment = 0;
    static float frequency = DEFAULT_SINE_WAVE_FREQUENCY;
    static float desired_frequency = frequency;
    static float volume = 0;
    static float desired_volume = volume;
    static float frequency_change_time = 0.05;
    static float volume_change_time = 0.2;
    static float frequency_delta_per_frame = 0;
    static float volume_delta_per_frame = 0;

    // render an interleaved output with the same sample value per channel
    // For example: 6 samples of a 2 channel output stream could look like this
    // 1,1,2,2,3,3

    // Only render full frames
    int frames = num_samples / num_audio_channels_;
    int sample_count = 0;

    if (soundDataArray != nullptr && soundLock.try_lock()) {

        // lock the Java lock and look for any changes from the Java layer
        if (java_lock.try_lock()) {
            // check for new preferences
            if (java_frequency_change_time != frequency_change_time) {
                frequency_change_time = java_frequency_change_time;
            }
            if (java_volume_change_time != volume_change_time) {
                volume_change_time = java_volume_change_time;
            }
            if (java_blend_mode != blend_mode) {
                blend_mode = java_blend_mode;
            }

            // check for new frequency
            if (java_frequency != desired_frequency) {
                desired_frequency = java_frequency;

                if (frequency_change_time > 0) {
                    frequency_delta_per_frame =
                            (desired_frequency - frequency) / frequency_change_time / frame_rate_;
                } else {
                    frequency_delta_per_frame = desired_frequency - frequency;
                }

                // frequency changed
                if (blend_mode == BLEND_TRANSITION) {
                    // move current primary sound to secondary
                    sound2_i = sound1_i;
                    sound2_index_increment = sound1_index_increment;

                    // set sound sound and reset increments after sound change
                    getBestSound(desired_frequency, sound1_i);
                    setIncrement(sound1_index_increment, sound1_i, frequency);

                    if (sound1_i == sound2_i) {
                        // no need to blend anything
                        sound2_i = -1; // invalidate identical secondary sound
                        sound1_volume = 1;
                        sound2_volume = 0;
                    } else {
                        // blend when actually frequency changes
                        sound1_volume = 0;
                        sound2_volume = 1;
                    }
                }
            }

            // check for new volume
            if (java_volume != desired_volume) {
                desired_volume = java_volume;

                if (volume_change_time > 0) {
                    volume_delta_per_frame =
                            (desired_volume - volume) / volume_change_time / frame_rate_;
                } else {
                    volume_delta_per_frame = desired_volume - volume;
                }
            }

            java_lock.unlock();
        }

        int16_t data;
        for (int i = 0; i < frames; i++) {
            data = 0;
            if (sound1_i != -1) data += (int16_t) (volume * sound1_volume * retrieve(sound1_i));
            if (sound2_i != -1) data += (int16_t) (volume * sound2_volume * retrieve(sound2_i));

            for (int j = 0; j < num_audio_channels_; j++) {
                audio_buffer[sample_count++] = data;
            }

            // if we are never blending, than change sound1_i when data crosses from negative to positive (sound2_i is always -1)
            if (blend_mode == BLEND_NEVER) {
                static int16_t previous = data;
                if (previous <= 0 && data >= 0) {
                    // set sound sound and reset increments after sound change
                    getBestSound(frequency, sound1_i);
                    setIncrement(sound1_index_increment, sound1_i, frequency);

                    sound2_i = -1;
                    sound1_volume = 1;
                    sound2_volume = 0;
                }
                previous = data;
            }

            // change our frequency if it is not as desired
            if (frequency != desired_frequency) {
                if (frequency_delta_per_frame *
                    ((frequency + frequency_delta_per_frame) - desired_frequency) >= 0) {
                    frequency = desired_frequency;
                } else {
                    frequency += frequency_delta_per_frame;
                }

                if (blend_mode == BLEND_ALWAYS) {
                    // recalculate best sounds for frequency change
                    getBestSound(frequency, sound1_i, sound2_i, sound1_volume, sound2_volume);

                    // sound1 is below the desired frequency and will be raised
                    // sound2 is above the desired frequency and will be lowered
                } else if (blend_mode == BLEND_TRANSITION) {
                    if (sound2_i != -1) {
                        float change_ratio = (frequency - desired_frequency) /
                                (frequency - frequency_delta_per_frame - desired_frequency);
                        sound2_volume *= change_ratio;
                        sound1_volume = 1 - sound2_volume;
                    }
                }

                // reset increments after frequency change
                setIncrement(sound1_index_increment, sound1_i, frequency);
                setIncrement(sound2_index_increment, sound2_i, frequency);
            }

            // change our volume if it is not as desired
            if (volume != desired_volume) {
                if (volume_delta_per_frame *
                    ((volume + volume_delta_per_frame) - desired_volume) >= 0) {
                    volume = desired_volume;
                } else {
                    volume += volume_delta_per_frame;
                }
            }

            // increment our sample position
            if (sound1_i != -1) samplePositions[sound1_i] += sound1_index_increment;
            if (sound2_i != -1) samplePositions[sound2_i] += sound2_index_increment;
        }

        soundLock.unlock();
    } else {
        for (int i = 0; i < frames; i++) {
            for (int j = 0; j < num_audio_channels_; j++) {
                audio_buffer[sample_count++] = 0;
            }
        }
    }

    Trace::endSection();

    return sample_count;
}

void Synthesizer::getBestSound(
        float frequency,
        int &sound1_i, int &sound2_i,
        float &sound1_volume, float &sound2_volume) {

    sound1_i = -1;
    sound2_i = -1;
    float sound1_best_margin = MAXFLOAT;
    float sound2_best_margin = MAXFLOAT;
    float margin;

    for (int sound_i = 0; sound_i < sounds_n; sound_i++) {
        margin = frequency - frequencyArray[sound_i];
        if (margin >= 0) {
            if (margin < sound1_best_margin) {
                sound1_i = sound_i;
                sound1_best_margin = margin;
            }
        } else {
            if (-margin < sound2_best_margin) {
                sound2_i = sound_i;
                sound2_best_margin = -margin;
            }
        }
    }

    sound1_volume = sound2_best_margin / (sound1_best_margin + sound2_best_margin);
    sound2_volume = sound1_best_margin / (sound1_best_margin + sound2_best_margin);
}

void Synthesizer::getBestSound(float frequency, int &sound1_i) {

    sound1_i = -1;
    float sound1_best_margin = MAXFLOAT;
    float margin;

    for (int sound_i = 0; sound_i < sounds_n; sound_i++) {
        margin = abs(frequency - frequencyArray[sound_i]);
        if (margin < sound1_best_margin) {
            sound1_i = sound_i;
            sound1_best_margin = margin;
        }
    }
}

void Synthesizer::setIncrement(float &sound_index_increment, int sound_i, float frequency) {
    if (sound_i != -1) {
        sound_index_increment = ((44100 * frequency) / (frame_rate_ * frequencyArray[sound_i]));
    }
}


float Synthesizer::retrieve(int sound_i) {
    // IF NEEDED: bring position back into range
    float &position = samplePositions[sound_i];
    int samples_n = soundSamples_n[sound_i];
    while (position >= samples_n) {
        position -= samples_n;
    }

    int position_int = (int) position;
    int16_t sample1 = soundDataArray[sound_i][position_int];
    int16_t sample2 = soundDataArray[sound_i][(position_int + 1 < samples_n ? position_int + 1
                                                                            : 0)];

    return sample1 + ((position - position_int) * (sample2 - sample1));
}

void Synthesizer::setVolume(float volume) {
    java_lock.lock();
    java_volume = volume;
    java_lock.unlock();
}

void Synthesizer::setFrequency(float frequency) {
    java_lock.lock();
    java_frequency = frequency;
    java_lock.unlock();
}

void Synthesizer::setPreferences(float frequency_change_time, float volume_change_time, int blend_mode) {
    java_lock.lock();
    java_frequency_change_time = frequency_change_time;
    java_volume_change_time = volume_change_time;
    java_blend_mode = blend_mode;
    java_lock.unlock();
}

void Synthesizer::setWorkCycles(int work_cycles) {
    work_cycles_ = work_cycles;
}

void Synthesizer::setSounds(JNIEnv *env, jobjectArray sound_data_array, jfloatArray frequency_array) {

    soundLock.lock();

    if (soundDataArray != nullptr) {
        // deallocate arrays in this object
        for (int sound_i = 0; sound_i < sounds_n; sound_i++) {
            delete[] soundDataArray[sound_i];
        }

        delete[] soundSamples_n;
        delete[] samplePositions;
        delete[] frequencyArray;
        delete[] soundDataArray;
        soundDataArray = nullptr;
    }

    // load sound set
    sounds_n = env->GetArrayLength(sound_data_array);

    // initialize arrays in this object
    soundDataArray = new int16_t *[sounds_n];
    frequencyArray = new float[sounds_n];
    samplePositions = new float[sounds_n];
    soundSamples_n = new int[sounds_n];

    jfloat *frequencies = env->GetFloatArrayElements(frequency_array, nullptr);

    for (int sound_i = 0; sound_i < sounds_n; sound_i++) {
        jshortArray sound_data = (jshortArray) env->GetObjectArrayElement(sound_data_array,
                                                                          sound_i);
        soundSamples_n[sound_i] = env->GetArrayLength(sound_data);
        jshort *sound_samples = env->GetShortArrayElements(sound_data, nullptr);

        // initialize elements of native arrays
        soundDataArray[sound_i] = new int16_t[soundSamples_n[sound_i]];
        frequencyArray[sound_i] = frequencies[sound_i];
        samplePositions[sound_i] = 0;

        for (int sound_sample_i = 0;
             sound_sample_i < soundSamples_n[sound_i]; sound_sample_i++) {
            // copy sound samples here
            soundDataArray[sound_i][sound_sample_i] = sound_samples[sound_sample_i];
        }

        env->ReleaseShortArrayElements(sound_data, sound_samples, JNI_ABORT);
    }

    env->ReleaseFloatArrayElements(frequency_array, frequencies, JNI_ABORT);

    LOGV("Loaded sounds");
    soundLock.unlock();

}

Synthesizer::~Synthesizer() {
    if (soundDataArray != nullptr) {
        // deallocate arrays in this object
        for (int sound_i = 0; sound_i < sounds_n; sound_i++) {
            delete[] soundDataArray[sound_i];
        }

        delete[] soundSamples_n;
        delete[] samplePositions;
        delete[] frequencyArray;
        delete[] soundDataArray;
        soundDataArray = nullptr;
    }
}
