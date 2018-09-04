(ns snowball.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log])
  (:refer-clojure :exclude [get]))

(defn get [{:keys [value]} & path]
  (get-in value path))

(defrecord Config []
  component/Lifecycle

  (start [this]
    (let [path "config.edn"]
      (log/info "Loading config from" path)
      (->> (io/resource path)
           (slurp)
           (edn/read-string)
           (assoc this :value))))

  (stop [this]
    (log/info "Cleaning up config")
    (assoc this :value nil)))
