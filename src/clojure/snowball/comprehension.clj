(ns snowball.comprehension
  (:require [clojure.core.async :as a]
            [taoensso.timbre :as log]
            [bounce.system :as b]
            [snowball.discord :as discord]
            [snowball.config :as config]
            [snowball.stream :as stream]
            [snowball.util :as util]
            [snowball.speech :as speech])
  (:import [snowball.porcupine Porcupine]
           [com.google.protobuf ByteString]
           [com.google.cloud.speech.v1p1beta1
            SpeechClient
            RecognitionConfig
            RecognitionAudio]))

(b/defcomponent phrase-audio-chan {:bounce/deps #{discord/audio-chan config/value}}
  (log/info "Starting phrase channel")
  (let [phrase-audio-chan (a/chan (a/sliding-buffer 100))
        state! (atom {})]

    (a/go-loop []
      ;; Pull a single audio packet out of Discord.
      ;; We receive one every 20ms and know which user it came from.
      (when-let [{:keys [user audio]} (a/<! discord/audio-chan)]
        (try
          ;; Insert some blank state for the user if there isn't any.
          ;; This means it's a new phrase.
          (when-not (@state! user)
            (swap! state! assoc user
                   (let [debounce-chan (a/chan)
                         out-chan (util/debounce debounce-chan (get-in config/value [:comprehension :phrase-debounce-ms]))]
                     (a/go
                       (a/>! phrase-audio-chan (a/<! out-chan))
                       (swap! state! dissoc user))
                     {:byte-stream (stream/byte-array-output)
                      :debounce-chan debounce-chan})))

          ;; Update the user's state by adding to their phrase.
          (let [{:keys [byte-stream debounce-chan]} (@state! user)]
            ;; Don't allow any more debounce refreshing if the vector gets too long.
            ;; This would be from someone playing music / talking for over a minute etc.
            (when (< (stream/size byte-stream) (get-in config/value [:comprehension :stream-bytes-cutoff]))
              ;; Add the new section of audio to the byte stream refresh the debounce.
              (stream/write byte-stream audio)

              ;; Update the debounced in channel.
              (a/>! debounce-chan {:byte-stream byte-stream, :user user})))
          (catch Exception e
            (log/error "Caught error in phrase-audio-chan loop" (Throwable->map e))))
        (recur)))

    (b/with-stop phrase-audio-chan
      (log/info "Closing phrase channel")
      (doseq [{:keys [debounce-chan]} (vals @state!)]
        (a/close! debounce-chan))
      (a/close! phrase-audio-chan))))

(defn byte-pair->short [[a b]]
  (bit-or (bit-shift-left a 8) (bit-and b 0xFF)))

(defn resample-for-porcupine [byte-stream]
  (->> byte-stream
       (stream/->bytes)
       (partition 2)
       (sequence (comp (take-nth 6) (map byte-pair->short)))
       (partition 512 512 (repeat 0))
       (map short-array)))

(defn resample-for-google [byte-stream]
  (->> byte-stream
       (stream/->bytes)
       (partition 2)
       (sequence (comp (take-nth 6) (map reverse)))
       (flatten)
       (byte-array)))

(b/defcomponent phrase-text-chan {:bounce/deps #{phrase-audio-chan speech/synthesiser}}
  (log/info "Starting speech to text systems")
  (let [speech-client (.. SpeechClient (create))
        recognition-config (.. RecognitionConfig
                               (newBuilder)
                               (setEncoding (.. com.google.cloud.speech.v1p1beta1.RecognitionConfig$AudioEncoding LINEAR16))
                               (setSampleRateHertz 16000)
                               (setLanguageCode "en-GB")
                               (build))
        phrase-text-chan (a/chan (a/sliding-buffer 100))
        porcupine (Porcupine. "wake-word-engine/Porcupine/lib/common/porcupine_params.pv"
                              "wake-word-engine/hey_snowball_linux.ppn"
                              0.5)
        frame-length (.getFrameLength porcupine)
        sample-rate (.getSampleRate porcupine)]

    (a/go-loop []
      ;; Wait for audio from a user in the voice channel.
      (when-let [{:keys [user byte-stream]} (a/<! phrase-audio-chan)]
        (try
          (let [frames (resample-for-porcupine byte-stream)]
            ;; Check if that audio contained the wake phrase using porcupine.
            (when (some #(.processFrame porcupine %) frames)
              (let [user-name (discord/user->name user)
                    timeout-chan (a/timeout (get-in config/value [:comprehension :post-wake-timeout-ms]))]
                (log/info (str "Woken by " user-name "."))
                (speech/say! (str "hey " user-name))

                ;; Wake phrase was spotted, so now we wait for a timeout or some more audio from that user.
                (loop []
                  (if-let [phrase (a/alt!
                                    timeout-chan nil
                                    phrase-audio-chan ([phrase] phrase))]
                    (if (= user (:user phrase))
                      (do
                        ;; TODO Add context to speech to text so it can understand user and channel names.

                        ;; Audio received from the wake phrase user, send it off to Google for recognition.
                        (log/info "Audio from" user-name "- sending to Google for speech recognition.")
                        (let [proto-bytes (.. ByteString (copyFrom (resample-for-google (:byte-stream phrase))))
                              recognition-audio (.. RecognitionAudio
                                                    (newBuilder)
                                                    (setContent proto-bytes)
                                                    (build))
                              results (-> (.. speech-client
                                              (recognize recognition-config recognition-audio)
                                              (getResultsList))
                                          (.iterator)
                                          (iterator-seq))]
                          ;; If we have a transcription result, put it onto the output channel.
                          (if (seq results)
                            (doseq [result results]
                              (let [transcript (.. result (getAlternativesList) (get 0) (getTranscript))]
                                (log/info  "Speech recognition transcript result:" transcript)

                                ;; TODO Remove this, it's a temporary parrot thing.
                                (speech/say! (str "I heard: " transcript))

                                (a/>! phrase-text-chan {:user user, :phrase transcript})))
                            (log/info "No results from Google speech recognition."))))
                      (recur))
                    (log/info user-name "didn't say anything after the wake word."))))))
          (catch Exception e
            (log/error "Caught error in phrase-text-chan loop" (Throwable->map e))))
        (recur)))

    (if (and (= frame-length 512) (= sample-rate 16000))
      (log/info (str "Porcupine frame length is 512 samples and the sample rate is 16KHz, as expected."))
      (throw (Error. (str "Porcupine frame length and sample rate should be 512 / 16000, got " frame-length " / " sample-rate " instead!"))))

    (b/with-stop phrase-text-chan
      (log/info "Shutting down speech to text systems")
      (.delete porcupine))))
