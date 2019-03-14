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
Sound sets are therefore used which are multiple recordings at several different frequencies, and the application chooses the recording to use based on how close its frequency is to the desired output frequency.
The goal is to minimize distortion.
When a note changes, however, the result is often that a separate audio buffer is played, and this results in a slight sound glitch as the buffer switches.
The application therefore changes frequency over a short period of time and blends the two sounds together, fading the first out while the second fades in, to provide a smooth transition between notes.
THis transition time will be provided as a setting in the future (see future features)

Updates:
* Frequency changes changes gradually and audio buffers blend for a smooth transition
* Volume changes gradually
* Sound sets all work on the native layer with instant switching between notes
* The delay moving from note to note has been fixed. Switching is now more or less instant.

Known issues:
* Player is not destroyed and created properly

Future features:
* Add settings option for volume change time
* Add settings option for frequency change time
* Control width of the ison dock
* Dark mode for when in church

## Please help me out!!
1. If you know of an issue, please add it and I will look into it. Feel free to request a feature as well!
1. Feel free to help me with coding as well. I am trying to comment my code better to make it more readable. I wrote this when I was in highschool so excuse my poor coding practices.

## Thanks for any help you can give!

### Thoughts moving forward
I have successfully moved all of the sound processing down to the native layer to improve the audio delay. Settings now needs to be added with dark theme and audio and layout settings.
