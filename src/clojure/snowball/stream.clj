(ns snowball.stream
  (:require [clojure.java.io :as io])
  (:import [java.io
            PipedInputStream
            PipedOutputStream
            ByteArrayOutputStream]))

(defn size [s]
  (.size s))

(defn output []
  (PipedOutputStream.))

(defn byte-array-output []
  (ByteArrayOutputStream.))

(defn ->bytes [s]
  (.toByteArray s))

(defn ->input-stream [x]
  (io/input-stream x))

(def stream-size (* 64 1024))

(defn input [output-stream]
  (PipedInputStream. output-stream stream-size))

(defn write [s bs]
  (.write s bs 0 (count bs)))
