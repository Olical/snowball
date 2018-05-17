(ns snowball.watch
  (:require [taoensso.timbre :as log]
            [snowball.watchers.presence :as presence]))

(defonce watch-future! (atom nil))

(defn check-all-watchers! []
  (presence/check!))

(defn start! [{:keys [poll-ms]}]
  (when @watch-future!
    (log/info "Killing existing watch loop")
    (future-cancel @watch-future!))

  (log/info "Starting watch loop, polls every" (str poll-ms "ms"))
  (->> (future
         (loop []
           (check-all-watchers!)
           (Thread/sleep poll-ms)
           (recur)))
       (reset! watch-future!)))
