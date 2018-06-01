(ns snowball.audio
  (:import [javax.sound.sampled AudioSystem]))

(defn input-stream [is]
  (AudioSystem/getAudioInputStream is))
