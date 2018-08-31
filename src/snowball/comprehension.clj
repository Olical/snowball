(ns snowball.comprehension
  (:require [taoensso.timbre :as log]
            [snowball.discord :as discord]
            [snowball.stream :as stream]))

(defonce subscription! (atom nil)) 
(defonce stream! (atom nil))

(defn handle-audio! [audio user]
  (when-not (discord/bot? user)
    (try
      (stream/write @stream! audio)
      (catch Exception e))))

(defn init! []
  (when @subscription!
    (log/info "Unsubscribing from existing audio")
    (discord/unsubscribe-audio! @subscription!))

  (when @stream!
    (log/info "Closing existing audio stream")
    (.close @stream!))

  (log/info "Subscribing to audio")
  (reset! subscription! (discord/subscribe-audio! handle-audio!))

  (reset! stream! (stream/output)))

(comment
  (init!))
