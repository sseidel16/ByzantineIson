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

#ifndef SIMPLESYNTH_SYNTHESIZER_H
#define SIMPLESYNTH_SYNTHESIZER_H

#include <cstdint>
#include <cmath>
#include <jni.h>
#include <mutex>
#include "audio_renderer.h"

class Synthesizer : public AudioRenderer {
private:
    int num_audio_channels_;
    int frame_rate_;
    int work_cycles_ = 0;

    int16_t **soundDataArray;
    float *frequencyArray;
    float *samplePositions;

    int sounds_n;
    int *soundSamples_n;

    // Java code passing in next frequency
    std::mutex frequencyLock;
    float nextFrequency;

    // Java code passing in next volume
    std::mutex volumeLock;
    float nextVolume;

    float retrieve(int sound_i);
    void getBestSound(float frequency, int& sound1_i, int& sound2_i,
            float& sound1_volume, float& sound2_volume);
public:
    Synthesizer(
            int num_audio_channels,
            int frame_rate,
            JNIEnv *env,
            jobjectArray sound_data_array,
            jfloatArray frequency_array);

    virtual int render(int num_samples, int16_t *audio_buffer);
    void setVolume(float volume);
    void setWaveFrequency(float wave_frequency);
    void setWorkCycles(int work_cycles);
    ~Synthesizer();
};

#endif //SIMPLESYNTH_SYNTHESIZER_H
