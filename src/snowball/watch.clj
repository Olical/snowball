(ns snowball.watch
  (:require [taoensso.timbre :as log]
            [snowball.util :as util]
            [snowball.config :as config]
            [snowball.watchers.presence :as presence]
            [snowball.watchers.five-queue :as five-queue]))

(defonce watch-future! (atom nil))

(defn check-all-watchers! []
  (presence/check!)
  (five-queue/check!))

(defn init! []
  (when @watch-future!
    (log/info "Killing existing watch loop")
    (future-cancel @watch-future!))

  (let [poll-ms (config/get :watch :poll-ms)]
    (log/info "Starting watch loop, polls every" (str poll-ms "ms"))
    (->> (future (util/poll-while poll-ms (constantly true) check-all-watchers!))
         (reset! watch-future!))))
