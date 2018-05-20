(ns snowball.comprehension
  (:require [taoensso.timbre :as log]
            [snowball.discord :as discord])
  (:import [java.io File FileInputStream InputStream]
           [edu.cmu.sphinx.api Configuration SpeechResult StreamSpeechRecognizer]))

;; goal:
;; store n bytes from every user in the channel right now
;; it's a moving buffer so I drop bytes as more are pushed on
;; every n ms (maybe 1s) that data is run through sphinx
;; if the keyword is spotted that user gets focus and can speak into the google API or whatever
;; when a keyword is seen we clear all buffers
;; should probably use tritonus for audio stuff?

(defonce subscription! (atom nil))

(defn handle-audio! [{:keys [audio user]}])

(defn init! []
  (when @subscription!
    (log/info "Unsubscribing from existing audio")
    (discord/unsubscribe-audio! @subscription!))

  (log/info "Subscribing to audio")
  (discord/subscribe-audio! #(handle-audio! %)))
