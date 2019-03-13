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

    setWaveFrequency(DEFAULT_SINE_WAVE_FREQUENCY);
    setVolume(0);
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
    static int sound_i = 0;
    static bool negative = true;

    // render an interleaved output with the same sample value per channel
    // For example: 6 samples of a 2 channel output stream could look like this
    // 1,1,2,2,3,3

    // Only render full frames
    int frames = num_samples / num_audio_channels_;
    int sample_count = 0;

    for (int i = 0; i < frames; i++) {
        int16_t data = (int16_t) (retrieve(sound_i) * current_volume_);
        for (int j = 0; j < num_audio_channels_; j++) {
            audio_buffer[sample_count++] = data;
        }

        samplePositions[sound_i] += 1;sample_index_increment_;

        // check for new sound if we crossed form negative to positive sample
        if (data > 0) {
            if (negative) {
                negative = false;

                // we just crossed the 0 threshold; try to lock and check for new sound
                if (frequencyLock.try_lock()) {
                    if (nextFrequency != frequency) {
                        frequency = nextFrequency;
                        sound_i = getBestSound(frequency);
                        sample_index_increment_ = ((44100 * frequency) / (frame_rate_ * frequencyArray[sound_i]));
                        LOGV("Increment: %f", sample_index_increment_);
                        LOGV("SoundI: %i", sound_i);
                    }
                    frequencyLock.unlock();
                }
            }
        } else {
            negative = true;
        }
    }

    Trace::endSection();

    return sample_count;
}

int Synthesizer::getBestSound(float frequency) {
    float logFrequency = log(frequency);
    int bestSound = 0;
    float bestMargin = abs(logFrequency - log(frequencyArray[0]));

    for (int sound_i = 1; sound_i < sounds_n; sound_i++) {
        float margin = abs(logFrequency - log(frequencyArray[sound_i]));
        if (margin < bestMargin) {
            bestMargin = margin;
            bestSound = sound_i;
        }
    }

    return bestSound;
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
    current_volume_ = volume < 1 ? volume : 1;
}

void Synthesizer::setWaveFrequency(float wave_frequency) {
    frequencyLock.lock();
    nextFrequency = wave_frequency;
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
