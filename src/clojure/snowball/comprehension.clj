(ns snowball.comprehension
  (:require [clojure.core.async :as a]
            [taoensso.timbre :as log]
            [bounce.system :as b]
            [snowball.discord :as discord]
            [snowball.config :as config]
            [snowball.stream :as stream]
            [snowball.util :as util])
  (:import [snowball.porcupine Porcupine]))

(b/defcomponent phrase-chan {:bounce/deps #{discord/audio-chan config/value}}
  (log/info "Starting phrase channel")
  (let [phrase-chan (a/chan (a/sliding-buffer 100))
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
                         db-out-chan (util/debounce debounce-chan (get-in config/value [:comprehension :phrase-debounce-ms]))]
                     (a/thread
                       (a/go
                         (a/>! phrase-chan (a/<! db-out-chan))
                         (swap! state! dissoc user)))
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
            (log/error "Caught error in phrase-chan loop" (Throwable->map e))))
        (recur)))

    (b/with-stop phrase-chan
      (log/info "Closing phrase channel")
      (doseq [{:keys [db-in-chan]} (vals @state!)]
        (a/close! db-in-chan))
      (a/close! phrase-chan))))

(defn resampled-frames [byte-stream]
  (->> byte-stream
       (stream/->bytes)
       (partition 2)
       (into [] (comp (take-nth 6)
                      (map reverse)))
       (flatten)
       (partition 1024 1024 (repeat 0))
       (map short-array)))

(b/defcomponent woken-by-chan {:bounce/deps #{phrase-chan}}
  (log/info "Starting Porcupine")
  (let [woken-by-chan (a/chan (a/sliding-buffer 100))
        porcupine (Porcupine. "wake-word-engine/Porcupine/lib/common/porcupine_params.pv"
                              "wake-word-engine/hey snowball_linux.ppn"
                              0.5)
        frame-length (.getFrameLength porcupine)
        sample-rate (.getSampleRate porcupine)]

    (a/go-loop []
      (when-let [{:keys [user byte-stream]} (a/<! phrase-chan)]
        (try
          (let [frames (resampled-frames byte-stream)]
            (prn "===" (.getName user) "frame count" (count frames))
            #_(prn (some #(.processFrame porcupine %) frames)))
          (catch Exception e
            (log/error "Caught error in woken-by-chan loop" (Throwable->map e))))
        (recur)))

    (if (and (= frame-length 512) (= sample-rate 16000))
      (log/info (str "Porcupine frame length is 512 samples and the sample rate is 16kHz, as expected."))
      (throw (Error. (str "Porcupine frame length and sample rate should be 512 / 16000, got " frame-length " / " sample-rate " instead!"))))

    (b/with-stop woken-by-chan
      (log/info "Shutting down Porcupine")
      (.delete porcupine))))
