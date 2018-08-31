# Snowball

Working on a voice based chatbot to sit in my Discord server with my friends.

Written in Clojure, feel free to fork and mess with it if you can work out how to use it. Built to run on GCP.

## Status

 * [x] Connecting to Discord
 * [x] Joining and leaving voice channels automatically when there are users around
 * [x] Synthesising speech using Google's API and sending it to a voice channel
 * [x] Receiving audio into a file (just interim until I can pipe through Sphinx / Google Speech API)
 * [ ] Comprehension of audio with CMU Sphinx for a keyword (requires downsampling to 16kHz and flipping BigEndian to LittleEndian)
 * [ ] Sending audio after a keyword off to Google for recognition (can use Discord's native 48kHz BigEndian)
 * [ ] Actual bot functionality using the comprehension and synthesis of speech developed earlier

## Prerequisites

 * Create `resources/config.edn`, there's an example file in the same directory.
 * Create `keys/google.json`, you can create a key file for your service account in your GCP dashboard.
 * Ensure [pocketsphinx][] is installed, I installed it through the Arch User Repository locally.

## Notes for development

 * Discord returns audio in `48KHz 16bit stereo signed BigEndian PCM`
 * Sphinx requires `RIFF (little-endian) data, WAVE audio, Microsoft PCM, 16 bit, mono 16000  Hz`
 * https://cmusphinx.github.io/wiki/tutorialsphinx4/
 * https://stackoverflow.com/questions/44772319/converting-raw-pcm-sound-java

![](images/snowball.png)

[pocketsphinx]: https://github.com/cmusphinx/pocketsphinx
