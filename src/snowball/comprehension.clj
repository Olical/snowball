(ns snowball.comprehension
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [snowball.discord :as discord]
            [snowball.stream :as stream]))

(defn handle-audio! [{:keys [stream]} audio user]
  (when-not (discord/bot? user)
    (let [bs (->> audio
                  (partition 2)
                  (into [] (comp (take-nth 6)
                                 (map reverse)))
                  (flatten)
                  (byte-array))]
      (stream/write stream bs))))

(defrecord Comprehension [discord]
  component/Lifecycle

  (start [this]
    (log/info "Subscribing to audio")
    (let [this (assoc this :stream (stream/byte-array-output))]
      (assoc this :subscription (discord/subscribe-audio! discord (partial handle-audio! this)))))

  (stop [{:keys [subscription stream] :as this}]
    (log/info "Unsubscribing from existing audio")
    (discord/unsubscribe-audio! discord subscription)

    (log/info "Closing existing audio stream")
    (.close stream)

    (assoc this
           :subscription nil
           :stream nil)))
