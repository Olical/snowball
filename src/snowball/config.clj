(ns snowball.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [taoensso.timbre :as log])
  (:refer-clojure :exclude [get]))

(defonce value! (atom nil))

(defn load! [path]
  (log/info "Loading config from" path)
  (->> (io/resource path)
       (slurp)
       (edn/read-string)
       (reset! value!)))

(defn get [& path]
  (get-in @value! path))
