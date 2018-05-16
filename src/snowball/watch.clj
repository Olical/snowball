(ns snowball.watch
  (:require [taoensso.timbre :as log]
            [snowball.discord :as discord]))

(defonce watch-future! (atom nil))

(defn upsert-channel! []
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

(defn check-all-watches! []
  (upsert-channel!))

(defn start! [{:keys [poll-ms]}]
  (when @watch-future!
    (log/info "Killing existing watch loop")
    (future-cancel @watch-future!))

  (log/info "Starting watch loop, polls every" (str poll-ms "ms"))
  (->> (future
         (loop []
           (check-all-watches!)
           (Thread/sleep poll-ms)
           (recur)))
       (reset! watch-future!)))
