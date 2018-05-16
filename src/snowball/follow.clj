(ns snowball.follow
  (:require [taoensso.timbre :as log]
            [snowball.discord :as discord]))

(def check-delay-ms 5000)
(defonce follower! (atom nil))

(defn check! []
  (let [desired-channel (->> (discord/channels!)
                             (sequence (comp (map (juxt identity discord/channel-users!))
                                             (remove (comp nil? second))
                                             (map first)))
                             (first))
        current-channel (discord/current-channel!)]
    (cond
      (and current-channel (nil? desired-channel)) (discord/leave! current-channel)
      (and desired-channel (not= current-channel desired-channel)) (discord/join! desired-channel))))

(defn start! []
  (log/info "Starting voice channel user follow loop")
  (->> (future
         (loop []
           (check!)
           (Thread/sleep check-delay-ms)
           (recur)))
       (reset! follower!)))
