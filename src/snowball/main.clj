(ns snowball.main
  (:require [taoensso.timbre :as log]
            [snowball.config :as config]
            [snowball.nrepl :as nrepl]
            [snowball.discord :as discord]
            [snowball.follow :as follow]))

(defn -main []
  (config/load! "config.edn")
  (nrepl/start! (config/get :nrepl))
  (discord/connect! (config/get :discord))
  (follow/start!)
  (log/info "Everything's up and running!"))
