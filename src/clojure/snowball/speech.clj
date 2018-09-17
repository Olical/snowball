(ns snowball.speech
  (:require [clojure.string :as str]
            [digest]
            [bounce.system :as b]
            [taoensso.timbre :as log]
            [snowball.audio :as audio]
            [snowball.stream :as stream]
            [snowball.discord :as discord]
            [snowball.config :as config])
  (:import [com.google.cloud.texttospeech.v1beta1
            TextToSpeechClient
            SynthesisInput
            VoiceSelectionParams
            AudioConfig
            SynthesizeSpeechResponse
            SsmlVoiceGender
            AudioEncoding]
           [com.google.cloud.storage
            Storage
            StorageOptions
            BlobInfo
            BlobId]))

(b/defcomponent cache {:bounce/deps #{config/value}}
  (log/info "Starting speech cache")
  (.. StorageOptions
      getDefaultInstance
      getService))

(defn object-name [s]
  (let [slug (-> s
                 (str/trim)
                 (str/lower-case)
                 (str/replace #"\s+" "-")
                 (str/replace #"[^\w\d\-]" ""))]
    (-> slug
        (subs 0 (min (count slug) 21))
        (str "-" (digest/sha-256 s)))))

(defn write-cache! [message data]
  (when-let [bucket (get-in config/value [:speech :cache :bucket])]
    (future
      (.. cache
          (create
            (.. BlobInfo (newBuilder bucket (object-name message)) build)
            data
            (make-array com.google.cloud.storage.Storage$BlobTargetOption 0))))))

(defn read-cache [message]
  (when-let [bucket (get-in config/value [:speech :cache :bucket])]
    (let [blob-id (.. BlobId (of bucket (object-name message)))
          blob (.. cache (get blob-id))]
      (when blob
        (.. blob
            (getContent (make-array com.google.cloud.storage.Blob$BlobSourceOption 0)))))))

(b/defcomponent synthesiser {:bounce/deps #{discord/client cache}}
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
  (if-let [cache-input-stream (read-cache message)]
    (-> cache-input-stream
        (stream/->input-stream)
        (audio/input->audio))
    (let [{:keys [client voice audio-config]} synthesiser
          input (.. SynthesisInput newBuilder (setText (str message)) build)
          response (.synthesizeSpeech client input voice audio-config)
          contents (.getAudioContent response)
          input-stream (.newInput contents)]
      (write-cache! message (stream/->bytes contents))
      (audio/input->audio input-stream))))

(defn say! [message]
  (future
    (->> (synthesise message)
         (discord/play!))))
