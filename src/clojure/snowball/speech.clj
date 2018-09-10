(ns snowball.speech
  (:require [bounce.system :as b]
            [taoensso.timbre :as log]
            [snowball.audio :as audio]
            [snowball.discord :as discord])
  (:import [com.google.cloud.texttospeech.v1beta1
            TextToSpeechClient
            SynthesisInput
            VoiceSelectionParams
            AudioConfig
            SynthesizeSpeechResponse
            SsmlVoiceGender
            AudioEncoding]))

(b/defcomponent synthesiser {:bounce/deps #{discord/client}}
  (log/info "Starting up speech client")
  (-> {:client (TextToSpeechClient/create)
       :voice (.. VoiceSelectionParams
                  newBuilder
                  (setLanguageCode "en_us")
                  (setSsmlGender SsmlVoiceGender/MALE)
                  build)
       :audio-config (.. AudioConfig
                         newBuilder
                         (setAudioEncoding AudioEncoding/MP3)
                         build)}
      (b/with-stop
        (log/info "Shutting down speech client")
        (.close (:client synthesiser)))))

(defn synthesise [message]
  (let [{:keys [client voice audio-config]} synthesiser
        input (.. SynthesisInput newBuilder (setText (str message)) build)
        response (.synthesizeSpeech client input voice audio-config)
        contents (.getAudioContent response)
        input-stream (.newInput contents)]
    (audio/input->audio input-stream)))

(defn say! [message]
  (future
    (->> (synthesise message)
         (discord/play!))))

(comment
  (say! "Hello, World!"))
