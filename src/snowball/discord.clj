(ns snowball.discord
  (:require [clojure.string :as str]
            [clojure.core.async :as a]
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

(declare client)

(defn ready? []
  (some-> client .isReady))

(defn poll-until-ready []
  (let [poll-ms (get-in config/value [:discord :poll-ms])]
    (log/info "Connected, waiting until ready")
    (util/poll-while poll-ms (complement ready?) #(log/info "Not ready, sleeping for" (str poll-ms "ms")))
    (log/info "Ready")))

(defn channels []
  (some-> client .getVoiceChannels seq))

(defn channel-users [channel]
  (some-> channel .getConnectedUsers seq))

(defn current-channel []
  (some-> client .getConnectedVoiceChannels seq first))

(defn leave! [channel]
  (when channel
    (log/info "Leaving" (.getName channel))
    (.leave channel)))

(defn join! [channel]
  (when channel
    (log/info "Joining" (.getName channel))
    (.join channel)))

(defn bot? [user]
  (some-> user .isBot))

(defn id [user]
  (some-> user .getLongID))

(defn muted? [user]
  (when user
    (let [voice-state (first (.. user getVoiceStates values))]
      (or (.isMuted voice-state)
          (.isSelfMuted voice-state)
          (.isSuppressed voice-state)))))

(defn can-speak? [user]
  (not (or (bot? user) (muted? user))))

(defn has-speaking-users? [channel]
  (->> (channel-users channel)
       (filter can-speak?)
       (seq)
       (boolean)))

(defn guilds []
  (some-> client .getGuilds seq))

(defn play! [audio]
  (when audio
    (when-let [guild (first (guilds))]
      (doto (AudioPlayer/getAudioPlayerForGuild guild)
        (.clear)
        (.queue audio)))))

(defmethod handle-event! :reconnect-success [_]
  (log/info "Reconnection detected, leaving any existing voice channels to avoid weird state")
  (poll-until-ready)
  (when-let [channel (current-channel)]
    (leave! channel)))

(defn audio-manager []
  (some-> (guilds) first .getAudioManager))

(defrecord AudioEvent [audio user])

(defn subscribe-audio! [f]
  (let [sub! (atom nil)]
    (a/go-loop []
      (a/<! (a/timeout (get-in config/value [:discord :poll-ms])))
      (if-let [am (audio-manager)]
        (let [sub (reify IAudioReceiver
                    (receive [_ audio user _ _]
                      (when-not (bot? user)
                        (f (AudioEvent. audio user)))))]
          (reset! sub! sub)
          (log/info "Audio manager exists, subscribing to audio")
          (.subscribeReceiver am sub))
        (recur)))

    (fn []
      (when-let [sub @sub!]
        (.unsubscribeReceiver (audio-manager) sub)))))

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

(b/defcomponent audio-chan {:bounce/deps #{client}}
  (log/info "Starting audio channel from subscription")
  (let [audio-chan (a/chan (a/sliding-buffer 100))
        sub (subscribe-audio!
              (fn [event]
                (a/go
                  (a/>! audio-chan event))))]
    (b/with-stop audio-chan
      (log/info "Closing audio channel and unsubscribing")
      (sub)
      (a/close! audio-chan))))
