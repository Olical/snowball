(ns snowball.main
  (:require [taoensso.timbre :as log]
            [snowball.config :as config]
            [snowball.nrepl :as nrepl]
            [snowball.discord :as discord]
            [snowball.watch :as watch]))

(defn -main []
  (config/load! "config.edn")
  (nrepl/start! (config/get :nrepl))
  (discord/connect! (config/get :discord))
  (watch/start! (config/get :watch))
  (log/info "Everything's up and running!"))
