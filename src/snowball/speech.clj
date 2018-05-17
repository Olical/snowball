(ns snowball.speech
  (:require [taoensso.timbre :as log]
            [snowball.discord :as discord])
  (:import [com.google.cloud.texttospeech.v1beta1
            TextToSpeechClient
            SynthesisInput
            VoiceSelectionParams
            AudioConfig
            SynthesizeSpeechResponse
            SsmlVoiceGender
            AudioEncoding]
           java.io.FileOutputStream
           javax.sound.sampled.AudioSystem))

(defonce client! (atom nil))
(defonce voice! (atom nil))
(defonce audio-config! (atom nil))

(defn init! []
  (log/info "Creating TTS client")
  (->> (TextToSpeechClient/create)
       (reset! client!))

  (log/info "Creating voice")
  (->> (.. VoiceSelectionParams
           newBuilder
           (setLanguageCode "en_us")
           (setSsmlGender SsmlVoiceGender/MALE)
           build)
       (reset! voice!))

  (log/info "Creating audio config")
  (->> (.. AudioConfig
           newBuilder
           (setAudioEncoding AudioEncoding/MP3)
           build)
       (reset! audio-config!)))

(defn synthesize [text]
  (let [input (.. SynthesisInput newBuilder (setText text) build)
        response (.synthesizeSpeech @client! input @voice! @audio-config!)
        contents (.getAudioContent response)
        input-stream (.newInput contents)]
    (AudioSystem/getAudioInputStream input-stream)))

(defn say! [text]
  (-> (synthesize text)
      (discord/play!)))
