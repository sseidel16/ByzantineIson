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

#include <assert.h>
#include <cstdlib>
#include "synthesizer.h"
#include "trace.h"
#include "android_log.h"

#define DEFAULT_SINE_WAVE_FREQUENCY 440.0

Synthesizer::Synthesizer(
        int num_audio_channels,
        int frame_rate,
        JNIEnv *env,
        jobjectArray sound_data_array,
        jfloatArray frequency_array) :
        num_audio_channels_(num_audio_channels),
        frame_rate_(frame_rate) {

    setWaveFrequency(DEFAULT_SINE_WAVE_FREQUENCY);
    setVolume(0);

    // load sound set
    sounds_n = env->GetArrayLength(sound_data_array);

    // initialize arrays in this object
    soundDataArray = new int16_t*[sounds_n];
    frequencyArray = new float[sounds_n];
    samplePositions = new float[sounds_n];
    soundSamples_n = new int[sounds_n];

    jfloat *frequencies = env->GetFloatArrayElements(frequency_array, 0);

    for (int sound_i = 0; sound_i < sounds_n; sound_i++) {
        jshortArray sound_data = (jshortArray) env->GetObjectArrayElement(sound_data_array, sound_i);
        soundSamples_n[sound_i] = env->GetArrayLength(sound_data);
        jshort *sound_samples = env->GetShortArrayElements(sound_data, 0);

        // initialize elements of native arrays
        soundDataArray[sound_i] = new int16_t[soundSamples_n[sound_i]];
        frequencyArray[sound_i] = frequencies[sound_i];
        samplePositions[sound_i] = 0;

        for (int sound_sample_i = 0; sound_sample_i < soundSamples_n[sound_i]; sound_sample_i++) {
            // copy sound samples here
            soundDataArray[sound_i][sound_sample_i] = sound_samples[sound_sample_i];
        }

        env->ReleaseShortArrayElements(sound_data, sound_samples, JNI_ABORT);
    }

    env->ReleaseFloatArrayElements(frequency_array, frequencies, JNI_ABORT);

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

    // this variable indicates whether the sound samples are positive or negative
    static int sound1_i = 0;
    static int sound2_i = 0;
    static float sound1_volume = 0;
    static float sound2_volume = 0;
    static float sound1_index_increment = 1;
    static float sound2_index_increment = 1;
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

    for (int i = 0; i < frames; i++) {
        int16_t data = (int16_t) (volume *
                (retrieve(sound1_i) * sound1_volume + retrieve(sound2_i) * sound2_volume)
        );
        for (int j = 0; j < num_audio_channels_; j++) {
            audio_buffer[sample_count++] = data;
        }

        // change our frequency if it is not as desired
        if (frequency != desired_frequency) {
            if (frequency_delta_per_frame *
                    ((frequency + frequency_delta_per_frame) - desired_frequency) >= 0) {
                frequency = desired_frequency;
            } else {
                frequency += frequency_delta_per_frame;
            }

            // recalculate best sounds for frequency change
            getBestSound(frequency, sound1_i, sound2_i, sound1_volume, sound2_volume);

            // sound1 is below the desired frequency and will be raised
            // sound2 is above the desired frequency and will be lowered
            sound1_index_increment = ((44100 * frequency) / (frame_rate_ * frequencyArray[sound1_i]));
            sound2_index_increment = ((44100 * frequency) / (frame_rate_ * frequencyArray[sound2_i]));
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
        if (sound1_volume > 0) samplePositions[sound1_i] += sound1_index_increment;
        if (sound2_volume > 0) samplePositions[sound2_i] += sound2_index_increment;
    }

    // check for new frequency
    if (frequencyLock.try_lock()) {
        if (nextFrequency != desired_frequency) {
            desired_frequency = nextFrequency;

            if (frequency_change_time > 0) {
                frequency_delta_per_frame = (desired_frequency - frequency) / frequency_change_time / frame_rate_;
            } else {
                frequency_delta_per_frame = desired_frequency - frequency;
            }
        }
        frequencyLock.unlock();
    }

    // check for new volume
    if (volumeLock.try_lock()) {
        if (nextVolume != desired_volume) {
            desired_volume = nextVolume;

            if (volume_change_time > 0) {
                volume_delta_per_frame = (desired_volume - volume) / volume_change_time / frame_rate_;
            } else {
                volume_delta_per_frame = desired_volume - volume;
            }
        }
        volumeLock.unlock();
    }

    Trace::endSection();

    return sample_count;
}

void Synthesizer::getBestSound(
        float frequency,
        int& sound1_i, int& sound2_i,
        float& sound1_volume, float& sound2_volume) {

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

    // ensure sounds are valid (volume will simply be 0 if sound index was -1)
    if (sound1_i < 0) {
        sound1_i = 0;
        sound1_volume = 0;
    }
    if (sound2_i < 0) {
        sound2_i = 0;
        sound2_volume = 0;
    }

}

float Synthesizer::retrieve(int sound_i) {
    // IF NEEDED: bring position back into range
    float &position = samplePositions[sound_i];
    int samples_n = soundSamples_n[sound_i];
    while (position >= samples_n) {
        position -= samples_n;
    }

    int position_int = (int)position;
    int16_t sample1 = soundDataArray[sound_i][position_int];
    int16_t sample2 = soundDataArray[sound_i][(position_int + 1 < samples_n ? position_int + 1 : 0)];

    return sample1 + ((position - position_int) * (sample2 - sample1));
}

void Synthesizer::setVolume(float volume) {
    volumeLock.lock();
    nextVolume = volume;
    volumeLock.unlock();
}

void Synthesizer::setWaveFrequency(float frequency) {
    frequencyLock.lock();
    nextFrequency = frequency;
    frequencyLock.unlock();
}

void Synthesizer::setWorkCycles(int work_cycles) {
    work_cycles_ = work_cycles;
}

Synthesizer::~Synthesizer() {
    // deallocate arrays in this object
    for (int sound_i = 0; sound_i < sounds_n; sound_i++) {
        delete[] soundDataArray[sound_i];
    }

    delete[] soundSamples_n;
    delete[] samplePositions;
    delete[] frequencyArray;
    delete[] soundDataArray;

}
