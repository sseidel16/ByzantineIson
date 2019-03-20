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

    std::mutex soundLock;
    int16_t **soundDataArray;

    // this array specifies the frequency for every sound sample array
    float *frequencyArray;

    // the current sample position/index in each sound sample array
    // This value is float because different frequencies may play fractions of a sample because of distortion
    float *samplePositions;
    int sounds_n;
    int *soundSamples_n;

    // Java code passing in frequency
    std::mutex java_lock;
    float java_frequency;
    float java_volume;
    float java_frequency_change_time;
    float java_volume_change_time;
    int java_blend_mode;

    float retrieve(int sound_i);
    void getBestSound(float frequency, int &sound1_i);
    void getBestSound(float frequency, int& sound1_i, int& sound2_i,
            float& sound1_volume, float& sound2_volume);
    void setIncrement(float &sound_index_increment, int sound_i, float frequency);

public:
    Synthesizer(int num_audio_channels, int frame_rate);

    virtual int render(int num_samples, int16_t *audio_buffer);
    void setVolume(float volume);
    void setFrequency(float frequency);
    void setPreferences(float frequency_change_time, float volume_change_time, int blend_mode);
    void setSounds(JNIEnv *env, jobjectArray sound_data_array, jfloatArray frequency_array);
    void setWorkCycles(int work_cycles);
    ~Synthesizer();
};

#endif //SIMPLESYNTH_SYNTHESIZER_H
