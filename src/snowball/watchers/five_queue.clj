(ns snowball.watchers.five-queue
  (:require [java-time :as jt]
            [snowball.discord :as discord]
            [snowball.speech :as speech]))

(defonce previous-size! (atom nil))
(defonce last-announcement! (atom nil))

(defn check! []
  (if-let [channel (discord/current-channel)]
    (let [channel-size (->> (discord/channel-users channel)
                            (filter discord/can-speak?)
                            (count))
          target 5]
      (when (and (= channel-size target)
                 (< @previous-size! target)
                 (or (nil? @last-announcement!) (jt/after? (jt/instant) (jt/plus @last-announcement! (jt/minutes 1)))))
        (speech/say! (str "That's the " target " queue!"))
        (reset! last-announcement! (jt/instant)))
      (reset! previous-size! channel-size))
    (do
      (reset! previous-size! nil)
      (reset! last-announcement! nil))))
