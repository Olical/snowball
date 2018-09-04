(ns snowball.watch
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [java-time :as jt]
            [snowball.util :as util]
            [snowball.config :as config]
            [snowball.discord :as discord]
            [snowball.speech :as speech]))

(defn check-presence! [{:keys [discord]}]
  (let [desired-channel (->> (discord/channels discord)
                             (filter discord/has-speaking-users?)
                             (first))
        current-channel (discord/current-channel discord)]

    (cond
      (and current-channel (nil? desired-channel))
      (discord/leave! discord current-channel)

      (and (or (nil? current-channel)
               (not (discord/has-speaking-users? current-channel)))
           desired-channel)
      (discord/join! desired-channel))))

(defn check-five-queue! [{:keys [discord speech previous-size! last-announcement!]}]
  (if-let [channel (discord/current-channel discord)]
    (let [channel-size (->> (discord/channel-users channel)
                            (filter discord/can-speak?)
                            (count))
          target 5]
      (when (and (= channel-size target)
                 (or (nil? @previous-size!) (< @previous-size! target))
                 (or (nil? @last-announcement!) (jt/after? (jt/instant) (jt/plus @last-announcement! (jt/hours 1)))))
        (speech/say! speech (str "That's the " target " queue!"))
        (log/info "Announced the five queue")
        (reset! last-announcement! (jt/instant)))
      (reset! previous-size! channel-size))
    (do
      (reset! previous-size! nil)
      (reset! last-announcement! nil))))

(defn check-all-watchers! [watch]
  (check-presence! watch)
  (check-five-queue! watch))

(defrecord Watch [discord config speech]
  component/Lifecycle

  (start [this]
    (let [poll-ms (config/get config :watch :poll-ms)
          this (assoc this
                      :previous-size! (atom nil)
                      :last-announcement! (atom nil))]
      (log/info "Starting watch loop, polls every" (str poll-ms "ms"))
      (assoc this :watch-future (future (util/poll-while poll-ms (constantly true) #(check-all-watchers! this))))))

  (stop [{:keys [watch-future] :as this}]
    (log/info "Killing existing watch loop")
    (future-cancel watch-future)
    (assoc this
           :previous-size! nil
           :last-announcement! nil
           :watch-future nil)))
