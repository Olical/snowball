(ns snowball.speech
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [snowball.discord :as discord]
            [snowball.audio :as audio])
  (:import [com.google.cloud.texttospeech.v1beta1
            TextToSpeechClient
            SynthesisInput
            VoiceSelectionParams
            AudioConfig
            SynthesizeSpeechResponse
            SsmlVoiceGender
            AudioEncoding]))

(defrecord Speech [discord]
  component/Lifecycle

  (start [this]
    (log/info "Starting up speech client")
    (assoc this
           :client (TextToSpeechClient/create)
           :voice (.. VoiceSelectionParams
                      newBuilder
                      (setLanguageCode "en_us")
                      (setSsmlGender SsmlVoiceGender/MALE)
                      build)
           :audio-config (.. AudioConfig
                             newBuilder
                             (setAudioEncoding AudioEncoding/MP3)
                             build)))

  (stop [{:keys [client] :as this}]
    (log/info "Shutting down speech client")
    (.close client)
    (assoc this
           :client nil
           :voice nil
           :audio-config nil)))

(defn synthesise [{:keys [client voice audio-config]} message]
  (let [input (.. SynthesisInput newBuilder (setText (str message)) build)
        response (.synthesizeSpeech client input voice audio-config)
        contents (.getAudioContent response)
        input-stream (.newInput contents)]
    (audio/input->audio input-stream)))

(defn say! [speech discord message]
  (future
    (->> (synthesise speech message)
         (discord/play! discord))))

(comment
  (let [{:keys [speech discord]} @snowball.main/system!]
    (say! speech discord "hi")))
