(ns snowball.comprehension
  (:require [bounce.system :as b]
            [taoensso.timbre :as log]
            [snowball.discord :as discord]
            [snowball.stream :as stream]))

(declare handle-audio!)

(b/defcomponent listener {:bounce/deps #{discord/client}}
  (log/info "Subscribing to discord audio")
  (-> {:stream (stream/byte-array-output)
       :subscription (discord/subscribe-audio! #'handle-audio!)}
      (b/with-stop
        (log/info "Unsubscribing from discord audio")
        (discord/unsubscribe-audio! (:subscription listener))
        (.close (:stream listener)))))

(defn handle-audio! [audio user]
  (when-not (discord/bot? user)
    (let [bs (->> audio
                  (partition 2)
                  (into [] (comp (take-nth 6)
                                 (map reverse)))
                  (flatten)
                  (byte-array))]
      (stream/write (:stream listener) bs))))
