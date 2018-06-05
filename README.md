# Snowball

Working on a voice based chatbot to sit in my Discord server with my friends.

Written in Clojure, feel free to fork and mess with it if you can work out how to use it. Built to run on GCP.

## Status

 * [x] Connecting to Discord
 * [x] Joining and leaving voice channels automatically when there are users around
 * [x] Synthesising speech using Google's API and sending it to a voice channel
 * [ ] Receiving audio - I can subscribe to audio but piecing the byte buffers together yields weird noises
 * [ ] Comprehension of audio with CMU Sphinx for a keyword
 * [ ] Sending audio after a keyword off to Google for recognition
 * [ ] Actual bot functionality using the comprehension and synthesis of speech developed earlier

![](images/snowball.png)
