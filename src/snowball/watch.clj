(ns snowball.watch
  (:require [taoensso.timbre :as log]
            [snowball.discord :as discord]))

(defonce watch-future! (atom nil))

(defn check! []
  (let [desired-channel (->> (discord/channels!)
                             (sequence (comp (map (juxt identity discord/channel-users!))
                                             (remove (comp nil? second))
                                             (map first)))
                             (first))
        current-channel (discord/current-channel!)]

    (cond
      (and current-channel (nil? desired-channel))
      (discord/leave! current-channel)

      (and desired-channel (not= current-channel desired-channel))
      (discord/join! desired-channel))))

(defn start! [{:keys [poll-ms]}]
  (when @watch-future!
    (log/info "Killing existing watch loop")
    (future-cancel @watch-future!))

  (log/info "Starting watch loop, polls every" (str poll-ms "ms"))

  (->> (future
         (loop []
           (check!)
           (Thread/sleep poll-ms)
           (recur)))
       (reset! watch-future!)))
