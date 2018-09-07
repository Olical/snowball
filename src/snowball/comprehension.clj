(ns snowball.comprehension
  (:require [clojure.core.async :as a]
            [taoensso.timbre :as log]
            [bounce.system :as b]
            [snowball.discord :as discord]
            [snowball.config :as config]
            [snowball.stream :as stream]
            ;; [snowball.audio :as audio]
            ))

(defn buffers->audio [buffers]
 (let [s (stream/byte-array-output)]
   (doseq [bs buffers]
     (stream/write s bs))
   ;; (audio/downsample (stream/->bytes s))
   (stream/->bytes s)))

(b/defcomponent phrase-chan {:bounce/deps #{discord/audio-chan config/value}}
  (log/info "Starting phrase channel")
  (let [phrase-chan (a/chan 100)
        state! (atom {})]
    (a/go-loop []
      ;; Pull a single audio packet out of Discord.
      ;; We receive one every 20ms and know which user it came from.
      (let [{:keys [user audio]} (a/<! discord/audio-chan)]
        ;; Update that particular user's state.
        (swap! state! update user
               (fnil (fn [{:keys [buffers debounce] :as s}]
                       ;; Kill any existing future.
                       (future-cancel debounce)

                       ;; Don't allow any more future refreshing if the vector gets too long.
                       ;; This would be from someone playing music / talking for over a minute etc.
                       (when (< (count buffers) (get-in config/value [:comprehension :buffer-cutoff-count]))
                         ;; Add the new section of audio to the buffers and create another future.
                         (-> s
                             (update :buffers conj audio)
                             (assoc :debounce (future
                                                ;; The future waits a chunk of time before continuing, gives time for a future-cancel.
                                                (Thread/sleep (get-in config/value [:comprehension :phrase-debounce-ms]))

                                                (let [{:keys [buffers]} (@state! user)]
                                                  ;; Render the buffers to audio and put it on the channel.
                                                  (a/>! phrase-chan {:user user
                                                                     :audio (buffers->audio buffers)})

                                                  ;; Remove the existing user key from the state.
                                                  (swap! state! dissoc user)))))))
                     {:buffers []
                      :debounce (future)}))
        (recur)))

    (b/with-stop phrase-chan
      (log/info "Closing phrase channel")
      (a/close! phrase-chan))))
