# Snowball

Working on a voice based chatbot to sit in my Discord server with my friends.

Written in Clojure, feel free to fork and mess with it if you can work out how to use it. Built to run on GCP.

## Status

 * [x] Connecting to Discord
 * [x] Joining and leaving voice channels automatically when there are users around
 * [x] Synthesising speech using Google's API and sending it to a voice channel
 * [x] Receiving audio into a file (just interim until I can pipe through Porcupine / Google Speech API)
 * [x] Batching streams of audio into distinct phrases grouped by user.
 * [x] Wake word detection with Porcupine (requires resampling the phrases)
 * [ ] Sending audio after a keyword off to Google for recognition
 * [ ] Actual bot functionality using the comprehension and synthesis of speech developed earlier

## Prerequisites

 * Create `resources/config.edn`, it's merged with `resources/config.base.edn` so you can set your own Discord token with `{:discord {:token "..."}}`.
 * Create `resources/google.json`, you can create a key file for your service account in your GCP dashboard.
 * The wake word detection is provided by [Porcupine][] and currently limited to Linux 64 bit. It requires some compilation that's run when you execute `make`.
 * To enable the synthesised speech cache, set `{:speech {:cache {:bucket "..."}}}` to a valid GCP bucket name in the config file.

## Running

After you've met the prerequisites listed above, simply run `make`. It'll compile and run everything required.

This will download the [Porcupine][] repository which is about 3gb last time I checked, you may be waiting around for a while.

## Notes on audio formats

 * Discord provides audio as `48KHz 16bit stereo signed BigEndian PCM`
 * Porcupine requires `16KHz 16bit mono signed LittleEndian PCM`
 * I _think_ GCP speech recognition can handle most PCM formats but I can use the same as Porcupine.

![](images/snowball.png)

[Porcupine]: https://github.com/picovoice/porcupine
