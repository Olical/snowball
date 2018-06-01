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

(defn bytes->audio [bs bitrate]
  (AudioInputStream. (ByteArrayInputStream. bs)
                     (AudioFormat. bitrate
                                   java.lang.Byte/SIZE
                                   1 true false)
                     (count bs)))

(defn write [audio-stream target]
  (AudioSystem/write audio-stream file-type (io/output-stream target)))
