# Snowball

A voice activated chat bot that sits in a [Discord][] server and joins audio channels with users. Written in [Clojure][] and using the [Google Cloud Platform][gcp] for speech comprehension and synthesis.

## Progress

 * [x] Connecting to Discord
 * [x] Joining and leaving voice channels automatically when there are users around
 * [x] Synthesising speech using Google's API and sending it to a voice channel
 * [x] Receiving audio into a file (just interim until I can pipe through Porcupine / Google Speech API)
 * [x] Batching streams of audio into distinct phrases grouped by user.
 * [x] Wake word detection with Porcupine (requires resampling the phrases)
 * [x] Sending audio after a keyword off to Google for recognition
 * [ ] Actual bot functionality using the comprehension and synthesis of speech developed earlier

## Prerequisites

 * Create `resources/config.edn`, it's merged with `resources/config.base.edn` so you can set your own Discord token with `{:discord {:token "..."}}`.
 * To enable the synthesised speech cache, set `{:speech {:cache {:bucket "..."}}}` to a valid GCP bucket name in the config file.
 * Create `resources/google.json`, you can create a key file for your service account in your GCP dashboard.
 * The wake word detection is provided by [Porcupine][] and currently limited to Linux 64 bit. It requires some compilation that's run when you execute `make`.

## Running

After you've met the prerequisites listed above, simply run `make`. It'll compile and run everything required.

This will download the [Porcupine][] repository which is about 3gb last time I checked, you may be waiting around for a while.

![](images/snowball.png)

[Porcupine]: https://github.com/picovoice/porcupine
[Discord]: https://discordapp.com/
[Clojure]: https://clojure.org/
[gcp]: https://cloud.google.com/
