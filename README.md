# Byzantine Ison Android Application
https://play.google.com/store/apps/details?id=com.coderss.ison

This Android application plays isokratima (ison) for chanters of Byzantine music.
It comes with 6 main scales and allows users to edit scales.

## Native audio
To provide lower audio latency, this application sends all sound samples to the native layer through the JNI.
This allows it bypass the delays in buffering samples at the Java layer and then passing these buffers down to the hardware in real time.
Once the application has loaded all samples in the native layer, it processes, manipulates, and streams audio to the speaker using native C and C++ code.
These files can be found in the cpp directory under main.

## Sound blending
The application can play any audio frequency by distorting recorded audio sounds.
However, the more these sounds are distorted, the less natural and more robotic the output sound becomes.
Sound sets are therefore used which are multiple recordings at several different frequencies, and the application chooses the recorded sounds to use based on how close their frequencies are to the desired output frequency.
The goal is to minimize distortion.
There are 3 sound blending modes provided by the application, that can be changed in preferences under Audio Preferences, and are listed below:

1. Always blend: This mode chooses the recorded buffer with the closest frequency below the desired frequency and the recorded buffer with the closest frequency above the desired frequency, and blends these 2 buffers together. The 2 buffers are not blended equally, but proportional to how close their frequencies are to the desired frequency. This mode provides a smooth sound across all frequencies and is the default mode.
2. Blend only on transition: Some sounds, such as a sine wave, do not blend well since one buffer ends up cancelling out part of the other buffer instead of just blending over it. In this scenario, it is often desireable to only play a single buffer. If the buffer changes due to a frequency change, however, the output sound glitches as the buffer switches. In this mode, the application changes frequency over a short period of time and blends the two buffers together, fading the first out while the second fades in, to provide a smooth transition between notes. This transition time is provided as a new preference (see updates).
3. Never blend: This mode only plays a single buffer no matter what. When the buffer switches, a small sound glitch will likely be heard.

Updates:

* Preference features added
  * Layout preferences
    * Preference for note button height
    * Preference for width of the ison dock
    * Dark and light theme preference (dark helps when in church)
  * Scale preferences
    * Preference for total buttons below base note
    * Preference for total buttons above base note
  * Audio preferences
    * Preference for base note slider (discrete vs continuous)
    * Preference for frequency change time
    * Preference for volume change time
    * Add preference for enable/disable sound blending
  * Advanced Layout Preferences
    * Preference for notes vertical and horizontal flow directions
    * Advanced layout preference for flow direction by row vs by column
* Sound set loading activity removed because performance drastically improved
* Frequency changes gradually and audio buffers blend for a smooth transition
* Volume changes gradually
* Sound sets all work on the native layer with instant switching between notes
* The delay moving from note to note has been fixed. Switching is now more or less instant

Known issues:

* Player is not destroyed and created properly
* This bug is being avoided and will not occur (it is kept here because it was not completely fixed and seemingly harmless code changes could trigger it again): Nasty bug where OpenSLES objects are not handled correctly causing the app to hang when navigating back and forth between pages a few times. This can freeze up the entire device and force a (simulated) battery pull so be careful.
* Scale manager does not handle its state well at all, especially on rotates
* Save button appears in scale manager even when there is nothing to save

Future features:

* Add preference for enable/disable play note on touch down

## Please help me out!!
1. If you know of an issue, please add it and I will look into it. Feel free to request a feature as well!
1. Feel free to help me with coding as well. I am trying to comment my code better to make it more readable. I wrote this when I was in highschool so excuse my poor coding practices.

## Thanks for any help you can give!

### Thoughts moving forward
I have successfully moved all of the sound processing down to the native layer to improve the audio delay. Settings now needs to be added with dark theme and audio and layout settings.
