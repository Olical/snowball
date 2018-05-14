(ns snowball.main
  (:require [taoensso.timbre :as log]
            [snowball.nrepl :as nrepl]
            [snowball.config :as config]))

(defn -main []
  (nrepl/start! 9001)
  (config/load! "config.edn")
  (log/info "Everything's up and running!"))
