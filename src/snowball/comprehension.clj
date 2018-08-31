(ns snowball.comprehension
  (:require [taoensso.timbre :as log]
            [snowball.discord :as discord]
            [snowball.stream :as stream])
  (:import [edu.cmu.sphinx.api Configuration StreamSpeechRecognizer]))

(defonce subscription! (atom nil)) 
(defonce sphinx! (atom nil))
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

  (when @sphinx!
    (log/info "Stopping existing sphinx")
    (.stopRecognition @sphinx!))

  (when @stream!
    (log/info "Closing existing audio stream")
    (.close @stream!))

  (log/info "Subscribing to audio")
  (reset! subscription! (discord/subscribe-audio! handle-audio!))

  (log/info "Booting CMUSphinx")
  (let [output-stream (stream/output)
        sphinx (StreamSpeechRecognizer.
                 (doto (Configuration.)
                   (.setAcousticModelPath "resource:/edu/cmu/sphinx/models/en-us/en-us")
                   (.setDictionaryPath "resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict")
                   (.setLanguageModelPath "resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin")))]
    (reset! sphinx! sphinx)
    (reset! stream! output-stream)
    (.startRecognition sphinx (stream/input output-stream))))

(comment
  (init!)
  (def result (.getResult @sphinx!))
  (.getHypothesis result))
