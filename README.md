# Snowball

A voice activated bot that sits in a [Discord][] server and joins audio channels with users.

## Example

This would take place entirely over voice, I'd just join a voice channel and Snowball would turn up.

> Olical: Hey Snowball
>
> Snowball: Hey Olical
>
> Olical: Move everyone to my channel
>
> Snowball: You got it.
>
> [everyone is moved to my channel]
>
> Olical: Hey Snowball
>
> Snowball: Hey Olical
>
> Olical: Play Darude Sandstorm 10 hours bass boosted
>
> Snowball: Sure
>
> [beautiful music starts playing]

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

You can run Snowball in two ways: Directly through the [Clojure CLI][cljcli] or by running the pre-built [Docker container][docker]. Before you start the application you need to create `config/config.edn` (which is merged on top of `config.base.edn`) and `config/google.json` which you can get from a service account in [GCP][].

Once you have your Google Cloud Platform account up and running you need to ensure the following services are activated for your account:

 * [Cloud Speech API][cloud-speech]
 * [Cloud Text-to-Speech API][cloud-text-to-speech]

You can also optionally activate [Cloud Storage][cloud-storage] and create a bucket that will be used for caching text to speech audio results.

Here's an example `config.edn` but you should be able to work this out yourself from the `nil` values in `config.base.edn`.

```edn
{:discord {:token "dkjdfd-my-discord-token-djslksdj"}        ;; This is the only required value.
 :speech {:cache {:bucket "your-speech-cache-bucket-name"}}
 :presence {:whitelist #{492031893908249384}}
 :command {:music {:channel 127661586692963623
                   :user 892362784653922700}}}
```

The presence whitelist and blacklist allow you to restrict what channels Snowball will join. The music and speech cache keys are completely optional, the only essential configuration is the discord token.

### Clojure

Once you have the CLI installed (it's available in most package managers) you can simply execute `make` to build and run the entire application. The first run will take a while because it has to download the [Porcupine][] repository which is fairly large. Make sure you have the core C build tools installed, a very small library is compiled for linking the JVM into Porcupine.

### Docker

If you know how to use Docker this'll be really straightforward, if you don't, go learn how to use Docker first. The tag in this block is the currently recommended version.

```bash
docker run -ti --rm -v (pwd)/config:/usr/snowball/config olical/snowball:ff3bb42ef2c22ff6197e311c230845738bf506fa
```

I run my instance on a Kubernetes cluster I created within GCP, I noted the steps I took in [`KUBE_NOTES.md`][kube-notes] which may help others get their own instances running cheaply.

## Commands

To give a command you need to say "hey snowball" and wait for acknowledgement, Snowball will say "hey [your name]" when it's ready. Then you can issue one of these commands. The matching is performed via regular expressions ([`snowball.command`][commands]) so you can use any sentence you want as long as the key words are present.

 * "say ..." - Repeats back to you whatever came after "say".
 * "who's a good boy?" - Confirms that Snowball, is indeed, a good boy.
 * "play ..." - Sends the `!play` command to the music bot with the song name that came after "play".
 * "volume" - Any sentence with volume in it triggers volume modification.
   * Including "increase", "up" or "raise" will raise the volume.
   * Including "decrease", "down" or "lower" will lower the volume.
   * Including a number will set the volume to that value.
   * Including an up / down word as well as a number will modify the volume by that amount.
     * "turn the volume up"
     * "lower the volume by 30"
     * "set the volume to 20"
 * "move" - Any sentence with move in it triggers channel movement.
   * You need to provide some users to move and a channel to move them to.
   * Usernames or "me", "myself" or "everyone" can be used to select people.
   * A channel name, "this channel", "my channel", "our channel" or "here" can be used to select a channel.
     * "move everyone to this channel"
     * "move me to channel 2"
     * "could you move Photan to the naughty corner"

There's a few more very obvious music bot commands.

 * pause / stop
 * resume / unpause
 * skip
 * summon
 * dismiss
 * clear

## Issues

If something isn't working quite right and there isn't an existing similar issue, please feel free to raise one with as mush information as you can provide. Logs, versions and system information all help.

Contributions are entirely welcome but also feel free to fork and modify this project to suit _your_ needs. Not every change needs to go into this repository, especially if you want to add meme commands that maybe only your Discord will appreciate.

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
[cljcli]: https://clojure.org/guides/getting_started
[cloud-speech]: https://console.cloud.google.com/apis/api/speech.googleapis.com/overview
[cloud-text-to-speech]: https://console.cloud.google.com/apis/api/texttospeech.googleapis.com/overview
[cloud-storage]: https://console.cloud.google.com/storage/browser
[kube-notes]: blob/master/KUBE_NOTES.md
[commands]: blob/master/src/clojure/snowball/command.clj
