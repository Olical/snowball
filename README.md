# Snowball

Working on a voice based chatbot to sit in my Discord server with my friends.

Written in Clojure, feel free to fork and mess with it if you can work out how to use it. Built to run on GCP.

## Status

 * [x] Connecting to Discord
 * [x] Joining and leaving voice channels automatically when there are users around
 * [x] Synthesising speech using Google's API and sending it to a voice channel
 * [x] Receiving audio into a file (just interim until I can pipe through Sphinx / Google Speech API)
 * [x] Batching streams of audio into distinct phrases grouped by user.
 * [ ] Wake word detection with Porcupine (requires resampling the phrases)
 * [ ] Sending audio after a keyword off to Google for recognition (can use Discord's native 48kHz BigEndian)
 * [ ] Actual bot functionality using the comprehension and synthesis of speech developed earlier

## Prerequisites

 * Create `resources/config.edn`, there's an example file in the same directory.
 * Create `resources/google.json`, you can create a key file for your service account in your GCP dashboard.

## Notes for development

 * Discord returns audio in `48KHz 16bit stereo signed BigEndian PCM`
 * Sphinx requires `RIFF (little-endian) data, WAVE audio, Microsoft PCM, 16 bit, mono 16000  Hz`
 * https://cmusphinx.github.io/wiki/tutorialsphinx4/
 * https://stackoverflow.com/questions/44772319/converting-raw-pcm-sound-java
 * https://github.com/jakebasile/clojure-jni-example
 * https://github.com/picovoice/porcupine

![](images/snowball.png)
