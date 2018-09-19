(ns snowball.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [bounce.system :as b]
            [taoensso.timbre :as log]
            [snowball.util :as util]))

(defn path->data [path]
  (->> path
       (slurp)
       (edn/read-string)))

(b/defcomponent value
  (let [base-path "config.base.edn"
        user-path "config/config.edn"]
    (log/info "Loading base config from" base-path "and user config from" user-path)
    (util/deep-merge (path->data base-path)
                     (path->data user-path))))
