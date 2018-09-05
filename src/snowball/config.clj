(ns snowball.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [bounce.system :as b]
            [taoensso.timbre :as log]))

(b/defcomponent value
  (let [path "config.edn"]
    (log/info "Loading config from" path)
    (->> (io/resource path)
         (slurp)
         (edn/read-string))))
