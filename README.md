# Snowball

A voice activated chat bot that sits in a [Discord][] server and joins audio channels with users.

## Key features

 * Control your music: `play hurricane highlife`, `turn the volume down`.
 * Move users around: `move me to channel 2`, `move everyone to my channel`, `move Olical and Photan to channel 1`.
 * Minimal required configuration.
 * Woken by "hey snowball", uses very little CPU.
 * Automatic joining and leaving of channels as users join, move and leave.
 * Easily runnable [Docker container][docker].

## Technology used

 * Main language: [Clojure][]
 * Discord API: [Discord4J][]
 * Wake word detection: [Porcupine][]
 * Speech comprehension, synthesis and cache: [Google Cloud Platform][gcp]
 * Music bot: [github.com/Just-Some-Bots/MusicBot][MusicBot]

## Running

TODO For now you can check out `KUBE_NOTES.md`.

## Usage

TODO But it's essentially `hey snowball`, wait for acknowledgement and then your command.

## Issues

TODO Report them?

![](images/snowball.png)

## Author

Built by [Oliver Caldwell][homepage] ([@OliverCaldwell][twitter]). Feel free to message me if you've got any feedback or questions.

## Unlicenced

Find the full [unlicense][] in the `UNLICENSE` file, but here's a snippet.

>This is free and unencumbered software released into the public domain.
>
>Anyone is free to copy, modify, publish, use, compile, sell, or distribute this software, either in source code form or as a compiled binary, for any purpose, commercial or non-commercial, and by any means.

Do what you want. Learn as much as you can. Unlicense more software.

[unlicense]: http://unlicense.org/
[Porcupine]: https://github.com/picovoice/porcupine
[Discord]: https://discordapp.com/
[Clojure]: https://clojure.org/
[gcp]: https://cloud.google.com/
[homepage]: https://oli.me.uk/
[twitter]: https://twitter.com/OliverCaldwell
[Discord4J]: https://github.com/Discord4J/Discord4J
[MusicBot]: https://github.com/Just-Some-Bots/MusicBot
[docker]: https://hub.docker.com/r/olical/snowball/
