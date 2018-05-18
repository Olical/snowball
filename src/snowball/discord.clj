(ns snowball.discord
  (:require [taoensso.timbre :as log])
  (:import sx.blah.discord.api.ClientBuilder
           sx.blah.discord.util.audio.AudioPlayer))

;; https://github.com/Discord4J/Discord4J
;; https://jitpack.io/com/github/Discord4J/Discord4J/2.10.1/javadoc/

(defonce client! (atom nil))
(defonce player! (atom nil))

(defn ready? []
  (.isReady @client!))

(defn connect! [{:keys [token poll-ms]}]
  (when @client!
    (log/info "Logging out of existing client")
    (.logout @client!))

  (log/info "Connecting to Discord")
  (->> (.. (ClientBuilder.)
           (withToken token)
           login)
       (reset! client!))

  (log/info "Connected, waiting until ready, polls every" (str poll-ms "ms"))
  (loop []
    (if (ready?)
      (log/info "Ready")
      (do
        (log/info "Not ready, sleeping for" (str poll-ms "ms"))
        (Thread/sleep poll-ms)
        (recur)))))

(defn channels []
  (seq (.getVoiceChannels @client!)))

(defn channel-users [channel]
  (seq (.getConnectedUsers channel)))

(defn current-channel []
  (-> (.getConnectedVoiceChannels @client!)
      (seq)
      (first)))

(defn leave! [channel]
  (log/info "Leaving" (.getName channel))
  (.leave channel))

(defn join! [channel]
  (log/info "Joining" (.getName channel))
  (.join channel))

(defn bot? [user]
  (.isBot user))

(defn muted? [user]
  (let [voice-state (first (.. user getVoiceStates values))]
    (or (.isMuted voice-state)
        (.isSelfMuted voice-state)
        (.isSuppressed voice-state))))

(defn can-speak? [user]
  (not (or (bot? user) (muted? user))))

(defn has-speaking-users? [channel]
  (->> (channel-users channel)
       (filter can-speak?)
       (seq)
       (boolean)))

(defn guilds []
  (seq (.getGuilds @client!)))

(defn play! [audio]
  (doto (AudioPlayer/getAudioPlayerForGuild (first (guilds)))
    (.clear)
    (.queue audio)))
