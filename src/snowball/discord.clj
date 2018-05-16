(ns snowball.discord
  (:require [taoensso.timbre :as log])
  (:import sx.blah.discord.api.ClientBuilder))

;; https://github.com/Discord4J/Discord4J
;; https://jitpack.io/com/github/Discord4J/Discord4J/2.10.1/javadoc/

(defonce client! (atom nil))

(defn connect! [{:keys [token poll-ms]}]
  (when @client!
    (log/info "Logging out of existing client")
    (.. @client! (logout)))

  (log/info "Connecting to Discord")
  (->> (.. (ClientBuilder.)
           (withToken token)
           (login))
       (reset! client!))

  (log/info "Connected, waiting until ready, polls every" (str poll-ms "ms"))
  (loop []
    (if (.. @client! (isReady))
      (log/info "Ready")
      (do
        (log/info "Not ready, sleeping for" (str poll-ms "ms"))
        (Thread/sleep poll-ms)
        (recur)))))

(defn channels []
  (seq (.. @client! (getVoiceChannels))))

(defn channel-users [channel]
  (seq (.. channel (getConnectedUsers))))

(defn current-channel []
  (-> (.. @client! (getConnectedVoiceChannels))
      (seq)
      (first)))

(defn leave! [channel]
  (log/info "Leaving" (.. channel (getName)))
  (.. channel (leave)))

(defn join! [channel]
  (log/info "Joining" (.. channel (getName)))
  (.. channel (join)))

(defn bot? [user]
  (.. user (isBot)))

(defn muted? [user]
  (let [voice-state (first (.. user (getVoiceStates) (values)))]
    (or (.. voice-state (isMuted))
        (.. voice-state (isSelfMuted))
        (.. voice-state (isSuppressed)))))

(defn has-speaking-users? [channel]
  (->> (channel-users channel)
       (remove #(or (bot? %) (muted? %)))
       (seq)
       (boolean)))
