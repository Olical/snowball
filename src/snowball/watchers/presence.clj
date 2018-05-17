(ns snowball.watchers.presence
  (:require [snowball.discord :as discord]))

(defn check! []
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
