(ns snowball.audio
  (:require [clojure.java.io :as io]
            [snowball.stream :as stream])
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

(defn stream->audio [s]
  (let [bs (stream/->bytes s)]
    (AudioInputStream. (ByteArrayInputStream. bs)
                       (AudioFormat. 16000 16 1 true false)
                       (count bs))))

(defn write [audio-stream target]
  (AudioSystem/write audio-stream file-type (io/output-stream target)))

(defn downsample [audio]
  (->> audio
       (partition 2)
       (into [] (comp (take-nth 6)
                      (map reverse)))
       (flatten)
       (byte-array)))
