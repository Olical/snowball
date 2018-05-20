(ns snowball.comprehension
  (:require [taoensso.timbre :as log]
            [snowball.discord :as discord])
  (:import [java.io PipedInputStream PipedOutputStream]
           [edu.cmu.sphinx.api Configuration StreamSpeechRecognizer]))

(def stream-size 1000)

(defonce subscription! (atom nil)) 
(defonce sphinx! (atom nil))
(defonce stream! (atom nil))

(defn handle-audio! [{:keys [user audio]}]
  (when-not (discord/bot? user)
    (.write @stream! audio 0 (count audio))))

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
  (reset! subscription! (discord/subscribe-audio! #(handle-audio! %)))

  (log/info "Booting CMUSphinx")
  (let [stream (PipedOutputStream.)
        sphinx (StreamSpeechRecognizer.
                 (doto (Configuration.)
                   (.setAcousticModelPath "resource:/edu/cmu/sphinx/models/en-us/en-us")
                   (.setDictionaryPath "resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict")
                   (.setLanguageModelPath "resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin")))]
    (reset! sphinx! sphinx)
    (reset! stream! stream)
    (.startRecognition sphinx (PipedInputStream. stream stream-size))))

(comment
  (def result (.getResult @sphinx!))
  (.getHypothesis result))
