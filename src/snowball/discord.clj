(ns snowball.discord
  (:require [clojure.string :as str]
            [bounce.system :as b]
            [taoensso.timbre :as log]
            [camel-snake-kebab.core :as csk]
            [snowball.config :as config]
            [snowball.util :as util])
  (:import [sx.blah.discord.api ClientBuilder]
           [sx.blah.discord.util.audio AudioPlayer]
           [sx.blah.discord.handle.audio IAudioReceiver]
           [sx.blah.discord.api.events IListener]))

(defn event->keyword [c]
  (-> (str c)
      (str/split #"\.")
      (last)
      (str/replace #"Event.*$" "")
      (csk/->kebab-case-keyword)))

(defmulti handle-event! (fn [c] (event->keyword c)))
(defmethod handle-event! :default [_])

(declare ready?)

(defn poll-until-ready []
  (let [poll-ms (get-in config/value [:discord :poll-ms])]
    (log/info "Connected, waiting until ready")
    (util/poll-while poll-ms (complement ready?) #(log/info "Not ready, sleeping for" (str poll-ms "ms")))
    (log/info "Ready")))

(b/defcomponent client {:bounce/deps #{config/value}}
  (log/info "Connecting to Discord")
  (let [token (get-in config/value [:discord :token])
        client (.. (ClientBuilder.)
                   (withToken token)
                   login)]

    (.registerListener
      (.getDispatcher client)
      (reify IListener
        (handle [_ event]
          (handle-event! event))))

    (with-redefs [client client]
      (poll-until-ready))

    (b/with-stop client
      (log/info "Shutting down Discord connection")
      (.logout client))))

(defn channels []
  (seq (.getVoiceChannels client)))

(defn channel-users [channel]
  (seq (.getConnectedUsers channel)))

(defn current-channel []
  (-> (.getConnectedVoiceChannels client)
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

(defn ready? []
  (.isReady client))

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
  (seq (.getGuilds client)))

(defn play! [audio]
  (doto (AudioPlayer/getAudioPlayerForGuild (first (guilds)))
    (.clear)
    (.queue audio)))

(defmethod handle-event! :reconnect-success [event]
  (log/info "Reconnection detected, leaving any existing voice channels to avoid weird state")
  (poll-until-ready)
  (when-let [channel (current-channel)]
    (leave! channel)))

(defn audio-manager []
  (-> (guilds) (first) (.getAudioManager)))

(defn subscribe-audio! [f]
  (let [am (audio-manager)
        subscription (reify IAudioReceiver
                       (receive [_ audio user _ _]
                         (f audio user)))]
    (.subscribeReceiver am subscription)
    subscription))

(defn unsubscribe-audio! [subscription]
  (let [am (audio-manager)]
    (.unsubscribeReceiver am subscription)))
