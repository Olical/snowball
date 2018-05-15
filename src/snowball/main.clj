(ns snowball.main
  (:require [taoensso.timbre :as log]
            [snowball.nrepl :as nrepl]
            [snowball.config :as config]
            [snowball.discord :as discord]))

(defn -main []
  (config/load! "config.edn")
  (nrepl/start! (config/get :nrepl))
  (discord/connect! (config/get :discord))
  (log/info "Everything's up and running!"))
