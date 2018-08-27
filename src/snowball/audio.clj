(ns snowball.audio
  (:require [clojure.java.io :as io])
  (:import [java.io ByteArrayInputStream]
           [javax.sound.sampled
            AudioSystem
            AudioInputStream
            AudioFormat
            AudioFileFormat]))

(def file-type
  javax.sound.sampled.AudioFileFormat$Type/WAVE)

(defn input->audio [input-stream]
  (AudioSystem/getAudioInputStream input-stream))

(defn bytes->discord-audio [bs]
  (AudioInputStream. (ByteArrayInputStream. bs)
                     (AudioFormat. 48000 16 2 true true)
                     (count bs)))

(defn bytes->sphinx-audio [bs-raw]
  (let [bs (->> bs-raw
                (partition 2)
                (take-nth 2)
                (take-nth 3)
                (map reverse)
                (flatten)
                (byte-array))]
    (AudioInputStream. (ByteArrayInputStream. bs)
                       (AudioFormat. 16000 16 1 true false)
                       (count bs))))

(defn write [audio-stream target]
  (AudioSystem/write audio-stream file-type (io/output-stream target)))
