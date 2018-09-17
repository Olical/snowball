(ns snowball.comprehension
  (:require [clojure.core.async :as a]
            [taoensso.timbre :as log]
            [bounce.system :as b]
            [snowball.discord :as discord]
            [snowball.config :as config]
            [snowball.stream :as stream]
            [snowball.util :as util]
            [snowball.speech :as speech])
  (:import [snowball.porcupine Porcupine]))

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

(defn resampled-frames [byte-stream]
  (->> byte-stream
       (stream/->bytes)
       (partition 2)
       (sequence (comp (take-nth 6) (map byte-pair->short)))
       (partition 512 512 (repeat 0))
       (map short-array)))

(b/defcomponent phrase-text-chan {:bounce/deps #{phrase-audio-chan speech/synthesiser}}
  (log/info "Starting Porcupine")
  (let [phrase-text-chan (a/chan (a/sliding-buffer 100))
        porcupine (Porcupine. "wake-word-engine/Porcupine/lib/common/porcupine_params.pv"
                              "wake-word-engine/hey_snowball_linux.ppn"
                              0.5)
        frame-length (.getFrameLength porcupine)
        sample-rate (.getSampleRate porcupine)]

    (a/go-loop []
      (when-let [{:keys [user byte-stream]} (a/<! phrase-audio-chan)]
        (try
          (let [frames (resampled-frames byte-stream)]
            (when (some #(.processFrame porcupine %) frames)
              (let [user-name (discord/user->name user)
                    timeout-chan (a/timeout (get-in config/value [:comprehension :post-wake-timeout-ms]))]
                (log/info (str "Woken by " user-name "."))
                (speech/say! (str "hey " user-name))

                (loop []
                  (if-let [phrase (a/alt!
                                    timeout-chan nil
                                    phrase-audio-chan ([phrase] phrase))]
                    (if (= user (:user phrase))
                      (log/info "TODO Send audio off to Google speech to text.")
                      (recur))
                    (log/info user-name "didn't say anything after the wake word."))))))
          (catch Exception e
            (log/error "Caught error in phrase-text-chan loop" (Throwable->map e))))
        (recur)))

    (if (and (= frame-length 512) (= sample-rate 16000))
      (log/info (str "Porcupine frame length is 512 samples and the sample rate is 16KHz, as expected."))
      (throw (Error. (str "Porcupine frame length and sample rate should be 512 / 16000, got " frame-length " / " sample-rate " instead!"))))

    (b/with-stop phrase-text-chan
      (log/info "Shutting down Porcupine")
      (.delete porcupine))))
