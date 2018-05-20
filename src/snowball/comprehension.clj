(ns snowball.comprehension
  (:require [taoensso.timbre :as log]
            [snowball.discord :as discord])
  (:import [java.io PipedInputStream PipedOutputStream]
           [edu.cmu.sphinx.api Configuration StreamSpeechRecognizer]))

(defonce subscription! (atom nil)) 

(defonce stream (PipedOutputStream.))

(defn handle-audio! [{:keys [user audio]}]
  (when-not (discord/bot? user)
    (.write stream audio 0 (count audio))))

(defn init! []
  (when @subscription!
    (log/info "Unsubscribing from existing audio")
    (discord/unsubscribe-audio! @subscription!))

  (log/info "Subscribing to audio")
  (discord/subscribe-audio! #(handle-audio! %)))

(comment
  (def r (StreamSpeechRecognizer.
           (doto (Configuration.)
             (.setAcousticModelPath "resource:/edu/cmu/sphinx/models/en-us/en-us")
             (.setDictionaryPath "resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict")
             (.setLanguageModelPath "resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin"))))

  (.startRecognition r (PipedInputStream. stream 1000))

  (def res (.getResult r))

  (.getHypothesis res)

  (.stopRecognition r))
