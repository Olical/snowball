(ns snowball.watch
  (:require [bounce.system :as b]
            [taoensso.timbre :as log]
            [java-time :as jt]
            [snowball.util :as util]
            [snowball.config :as config]
            [snowball.discord :as discord]
            [snowball.speech :as speech]))

(declare check-all-watchers!)

(b/defcomponent poller {:bounce/deps #{discord/client config/value speech/synthesiser}}
  (let [poll-ms (get-in config/value [:watch :poll-ms])]
    (log/info "Starting watch loop, polls every" (str poll-ms "ms"))
    (-> {:previous-size! (atom nil)
         :last-announcement! (atom nil)
         :watch-future (future (util/poll-while poll-ms (constantly true) #'check-all-watchers!))}
        (b/with-stop
          (log/info "Killing existing watch loop")
          (future-cancel (:watch-future poller))))))

(defn check-presence! []
  (let [desired-channel (->> (discord/channels)
                             (filter discord/has-speaking-users?)
                             (first))
        current-channel (discord/current-channel)]

    (cond
      (and current-channel (nil? desired-channel))
      (discord/leave! current-channel)

      (and (or (nil? current-channel)
               (not (discord/has-speaking-users? current-channel)))
           desired-channel)
      (discord/join! desired-channel))))

(defn check-five-queue! []
  (let [{:keys [previous-size! last-announcement!]} poller]
    (if-let [channel (discord/current-channel)]
      (let [channel-size (->> (discord/channel-users channel)
                              (filter discord/can-speak?)
                              (count))
            target 5]
        (when (and (= channel-size target)
                   (or (nil? previous-size!) (< previous-size! target))
                   (or (nil? last-announcement!) (jt/after? (jt/instant) (jt/plus last-announcement! (jt/hours 1)))))
          (speech/say! (str "That's the " target " queue!"))
          (log/info "Announced the five queue")
          (reset! last-announcement! (jt/instant)))
        (reset! previous-size! channel-size))
      (do
        (reset! previous-size! nil)
        (reset! last-announcement! nil)))))

(defn check-all-watchers! []
  (check-presence!)
  (check-five-queue!))
