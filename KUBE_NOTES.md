# Snowball on GCP via Kube

This file is a list of steps I had to take to get Snowball running on Google Cloud Platform through their managed Kubernetes infrastructure. It's partially for my future reference but it may help you out if you're trying to do something similar.

```bash
# Basic settings
gcloud config set project snowball-[redacted]
gcloud config set compute/zone europe-west4-b

# Create a minimal cluster
# TODO: Use a smaller disk
gcloud container clusters create snowball-cluster --num-nodes=1 --preemptible

# Configure kubectl for the new cluster
gcloud container clusters get-credentials snowball-cluster 

# Add the config to the new cluster
# Make sure you've created config/config.edn and config/google.json first!
kubectl create configmap snowball-config --from-file config/

# You can update that config with this
# I suppose you could also just delete it and re-create it too
kubectl create configmap snowball-config --from-file config/ -o yaml --dry-run | kubectl replace -f -

# Deploy the container
kubectl run snowball --image=olical/snowball:...

# Update the deployment and add the YAML below, it maps the config into the container
kubectl edit deployments snowball

# Check the logs with this
kubectl logs deployment/snowball -f

# Update to another sha with this
kubectl set image deployment/snowball snowball=olical/snowball:...
```

## YAML for `kubectl edit deployments snowball`

We add `volumeMounts` and `volumes`.

```yaml
    spec:
      containers:
      - image: olical/snowball
        imagePullPolicy: Always
        name: snowball
        resources: {}
        terminationMessagePath: /dev/termination-log
        terminationMessagePolicy: File
        volumeMounts:
        - name: config-volume
          mountPath: /usr/snowball/config
      dnsPolicy: ClusterFirst
      restartPolicy: Always
      schedulerName: default-scheduler
      securityContext: {}
      terminationGracePeriodSeconds: 30
      volumes:
        - name: config-volume
          configMap:
            name: snowball-config
```

## MusicBot

The music commands are designed for [MusicBot][], you can run that on your Kubernetes cluster too.

```bash
# Create some configuration based on the YAML below
kubectl create configmap musicbot-config --from-file options.ini

# Deploy the container
kubectl run musicbot --image=justsomebots/musicbot:1.9.8

# Update the YAML just like snowball and add the new config map, example below
kubectl edit deployments musicbot
```

## MusicBot config YAML

```yaml
[Credentials]
# This is your Discord bot account token.
# Find your bot's token here: https://discordapp.com/developers/applications/me/
# Create a new application, with no redirect URI or boxes ticked.
# Then click 'Create Bot User' on the application page and copy the token here.
Token = "..."

# The bot supports converting Spotify links and URIs to YouTube videos and
# playing them. To enable this feature, please fill in these two options with valid
# details, following these instructions: https://just-some-bots.github.io/MusicBot/using/spotify/
Spotify_ClientID =
Spotify_ClientSecret =

[Permissions]
# This option determines which user has full permissions and control of the bot.
# You can only set one owner, but you can use permissions.ini to give other
# users access to more commands.
# Setting this option to 'auto' will set the owner of the bot to the person who
# created the bot application, which is usually what you want. Else, change it
# to another user's ID.
OwnerID = auto

# This option determines which users have access to developer-only commands.
# Developer only commands are very dangerous and may break your bot if used
# incorrectly, so it's highly recommended that you ignore this option unless you
# are familiar with Python code.
DevIDs =

[Chat]
# Determines the prefix that must be used before commands in the Discord chat.
# e.g if you set this to *, the play command would be triggered using *play.
CommandPrefix = !

# Restricts the bot to only listening to certain text channels. To use this, add
# the IDs of the text channels you would like the bot to listen to, seperated by
# a space.
BindToChannels =

# Allows the bot to automatically join servers on startup. To use this, add the IDs
# of the voice channels you would like the bot to join on startup, seperated by a
# space. Each server can have one channel. If this option and AutoSummon are
# enabled, this option will take priority.
AutojoinChannels =

[MusicBot]
# The volume of the bot, between 0.01 and 1.0.
DefaultVolume = 0.25

# Only allows whitelisted users (in whitelist.txt) to use commands.
# WARNING: This option has been deprecated and will be removed in a future version
# of the bot. Use permissions.ini instead.
WhiteListCheck = no

# The number of people voting to skip in order for a song to be skipped successfully,
# whichever value is lower will be used. Ratio refers to the percentage of undefeaned, non-
# owner users in the channel. 
SkipsRequired = 1
SkipRatio = 0.5

# Determines if downloaded videos will be saved to the audio_cache folder. If this is yes,
# they will not be redownloaded if found in the folder and queued again. Else, videos will
# be downloaded to the folder temporarily to play, then deleted after to avoid filling space.
SaveVideos = yes

# Mentions the user who queued a song when it starts to play.
NowPlayingMentions = no

# Automatically joins the owner's voice channel on startup, if possible. The bot must be on
# the same server and have permission to join the channel.
AutoSummon = yes

# Start playing songs from the autoplaylist.txt file after joining a channel. This does not
# stop users from queueing songs, you can do that by restricting command access in permissions.ini.
UseAutoPlaylist = no

# Sets if the autoplaylist should play through songs in a random order when enabled. If no,
# songs will be played in a sequential order instead.
AutoPlaylistRandom = yes

# Pause the music when nobody is in a voice channel, until someone joins again.
AutoPause = yes

# Automatically cleanup the bot's messages after a small period of time.
DeleteMessages = yes

# If this and DeleteMessages is enabled, the bot will also try to delete messages from other
# users that called commands. The bot requires the 'Manage Messages' permission for this.
DeleteInvoking = no

# Regularly saves the queue to the disk. If the bot is then shut down, the queue will
# resume from where it left off.
PersistentQueue = yes

# Determines what messages are logged to the console. The default level is INFO, which is
# everything an average user would need. Other levels include CRITICAL, ERROR, WARNING,
# DEBUG, VOICEDEBUG, FFMPEG, NOISY, and EVERYTHING. You should only change this if you
# are debugging, or you want the bot to have a quieter console output.
DebugLevel = INFO

# Specify a custom message to use as the bot's status. If left empty, the bot
# will display dynamic info about music currently being played in its status instead.
StatusMessage = 

# Write what the bot is currently playing to the data/<server id>/current.txt FILE.
# This can then be used with OBS and anything else that takes a dynamic input.
WriteCurrentSong = no

# Allows the person who queued a song to skip their OWN songs instantly, similar to the
# functionality that owners have where they can skip every song instantly.
AllowAuthorSkip = yes

# Enables experimental equalization code. This will cause all songs to sound similar in
# volume at the cost of higher processing consumption when the song is initially being played.
UseExperimentalEqualization = no

# Enables the use of embeds throughout the bot. These are messages that are formatted to
# look cleaner, however they don't appear to users who have link previews disabled in their
# Discord settings.
UseEmbeds = yes

# The amount of items to show when using the queue command.
QueueLength = 10

# Remove songs from the autoplaylist if an error occurred while trying to play them.
# If enabled, unplayable songs will be moved to another file and out of the autoplaylist.
# You may want to disable this if you have internet issues or frequent issues playing songs.
RemoveFromAPOnError = yes

# Whether to show the configuration for the bot in the console when it launches.
ShowConfigOnLaunch = no

# Whether to use leagcy skip behaviour. This will change it so that those with permission
# do not need to use "skip f" to force-skip a song, they will instead force-skip by default.
LegacySkip = no

[Files]
# Path to your i18n file. Do not set this if you do not know what it does.
i18nFile = 
```

## MusicBot deployment YAML for config mountin

```yaml
    spec:
      containers:
      - image: justsomebots/musicbot:1.9.8
        imagePullPolicy: Always
        name: musicbot
        resources: {}
        terminationMessagePath: /dev/termination-log
        terminationMessagePolicy: File
        volumeMounts:
        - name: config-volume
          mountPath: /usr/src/musicbot/config/options.ini
      dnsPolicy: ClusterFirst
      restartPolicy: Always
      schedulerName: default-scheduler
      securityContext: {}
      terminationGracePeriodSeconds: 30
      volumes:
        - name: config-volume
          configMap:
            name: musicbot-config
```

[MusicBot]: https://github.com/Just-Some-Bots/MusicBot
