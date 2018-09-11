(ns snowball.util
  (:require [clojure.core.async :as a]
            [taoensso.timbre :as log]))

(defn poll-while [poll-ms pred-fn body-fn]
  (loop []
    (when (try
            (pred-fn)
            (catch Exception e
              (log/error "Caught an error in poll-while pred" (Throwable->map e))
              (.printStackTrace e)
              true))
      (try
        (body-fn)
        (catch Exception e
          (log/error "Caught an error in poll-while body" (Throwable->map e))
          (.printStackTrace e)))
      (Thread/sleep poll-ms)
      (recur))))

;; https://gist.github.com/scttnlsn/9744501
(defn debounce [in ms]
  (let [out (a/chan)]
    (a/go-loop [last-val nil]
      (let [val (if (nil? last-val) (a/<! in) last-val)
            timer (a/timeout ms)
            [new-val ch] (a/alts! [in timer])]
        (condp = ch
          timer (do
                  (a/>! out val)
                  (recur nil))
          in (when new-val
               (recur new-val)))))
    out))

