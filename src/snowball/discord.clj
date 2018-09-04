(ns snowball.discord
  (:require [clojure.string :as str]
            [com.stuartsierra.component :as component]
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

(defmulti handle-event! (fn [_ c] (event->keyword c)))
(defmethod handle-event! :default [_ _])

(declare ready?)

(defn poll-until-ready [{:keys [config] :as discord}]
  (let [poll-ms (config/get config :discord :poll-ms)]
    (log/info "Connected, waiting until ready")
    (util/poll-while poll-ms #(not (ready? discord)) #(log/info "Not ready, sleeping for" (str poll-ms "ms")))
    (log/info "Ready")))

(defrecord Discord [config]
  component/Lifecycle

  (start [this]
    (log/info "Connecting to Discord")
    (let [token (config/get config :discord :token)
          client (.. (ClientBuilder.)
                     (withToken token)
                     login)
          this (assoc this :client client)]

      (.registerListener
        (.getDispatcher client)
        (reify IListener
          (handle [_ event]
            (handle-event! this event))))

      (poll-until-ready this)

      this))

  (stop [{:keys [client] :as this}]
    (log/info "Logging out of existing client")
    (.logout client)
    (assoc this :client nil)))

(defn channels [{:keys [client]}]
  (seq (.getVoiceChannels client)))

(defn channel-users [channel]
  (seq (.getConnectedUsers channel)))

(defn current-channel [{:keys [client]}]
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

(defn ready? [{:keys [client]}]
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

(defn guilds [{:keys [client]}]
  (seq (.getGuilds client)))

(defn play! [discord audio]
  (doto (AudioPlayer/getAudioPlayerForGuild (first (guilds discord)))
    (.clear)
    (.queue audio)))

(defmethod handle-event! :reconnect-success [discord event]
  (log/info "Reconnection detected, leaving any existing voice channels to avoid weird state")
  (poll-until-ready discord)
  (when-let [channel (current-channel discord)]
    (leave! channel)))

(defn audio-manager [discord]
  (-> (guilds discord) (first) (.getAudioManager)))

(defn subscribe-audio! [discord f]
  (let [am (audio-manager discord)
        subscription (reify IAudioReceiver
                       (receive [_ audio user _ _]
                         (f audio user)))]
    (.subscribeReceiver am subscription)
    subscription))

(defn unsubscribe-audio! [discord subscription]
  (let [am (audio-manager discord)]
    (.unsubscribeReceiver am subscription)))
