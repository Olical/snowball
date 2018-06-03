(ns snowball.discord
  (:require [clojure.string :as str]
            [taoensso.timbre :as log]
            [camel-snake-kebab.core :as csk]
            [snowball.util :as util]
            [snowball.config :as config]
            [snowball.stream :as stream]
            [snowball.audio :as audio])
  (:import [sx.blah.discord.api ClientBuilder]
           [sx.blah.discord.util.audio AudioPlayer]
           [sx.blah.discord.handle.audio IAudioReceiver]
           [sx.blah.discord.api.events IListener]))

(defonce client! (atom nil))
(defonce player! (atom nil))

(defn event->keyword [c]
  (-> (str c)
      (str/split #"\.")
      (last)
      (str/replace #"Event.*$" "")
      (csk/->kebab-case-keyword)))

(defmulti handle-event! event->keyword)
(defmethod handle-event! :default [event])

(declare ready?)

(defn poll-until-ready []
  (let [poll-ms (config/get :discord :poll-ms)]
    (log/info "Connected, waiting until ready")
    (util/poll-while poll-ms #(not (ready?)) #(log/info "Not ready, sleeping for" (str poll-ms "ms")))
    (log/info "Ready")))

(defn init! []
  (when @client!
    (log/info "Logging out of existing client")
    (.logout @client!))

  (log/info "Connecting to Discord")
  (let [token (config/get :discord :token)]
    (->> (.. (ClientBuilder.)
             (withToken token)
             login)
         (reset! client!)))

  (.registerListener
    (.getDispatcher @client!)
    (reify IListener
      (handle [this event]
        (handle-event! event))))

  (poll-until-ready))

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

(defn ready? []
  (.isReady @client!))

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
                       (receive [this audio user seq-char timestamp]
                         (f {:audio audio
                             :user user
                             :seq-char seq-char
                             :timestamp timestamp})))]
    (.subscribeReceiver am subscription)
    subscription))

(defn unsubscribe-audio! [subscription]
  (let [am (audio-manager)]
    (.unsubscribeReceiver am subscription)))

(comment
  (def out (stream/byte-array-output))

  (def previous! (atom 0))

  (defn handler [{:keys [audio user timestamp]}]
    (when-not (bot? user)
      (println (- timestamp @previous!))
      (reset! previous! timestamp)
      (stream/write out audio)))

  (unsubscribe-audio! sub)
  (def sub (subscribe-audio! (fn [event] (handler event))))

  (audio/write
    (audio/bytes->audio (stream/->bytes out)
                        (.getBitrate (current-channel)))
    (clojure.java.io/output-stream "out.wav")))
