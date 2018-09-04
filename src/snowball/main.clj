(ns snowball.main
  (:require [clojure.edn :as edn]
            [clojure.tools.nrepl.server :as nrepl]
            [cider.nrepl]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [snowball.config :as config]
            [snowball.discord :as discord]
            [snowball.watch :as watch]
            [snowball.speech :as speech]
            [snowball.comprehension :as comprehension]))

(defonce system! (atom nil))

(defn start! []
  (let [system (component/system-map
                 :config (config/map->Config {})
                 :discord (component/using
                            (discord/map->Discord {})
                            [:config])
                 :watch (component/using
                          (watch/map->Watch {})
                          [:config :discord :speech])
                 :speech (component/using
                           (speech/map->Speech {})
                           [:discord])
                 :comprehension (component/using
                                  (comprehension/map->Comprehension {})
                                  [:discord]))]
    (log/info "Starting components")
    (reset! system! (component/start system))
    (log/info "Everything's up and running!")))

(defn stop! []
  (log/info "Stopping components")
  (component/stop @system!)
  (reset! system! nil)
  (log/info "Components are shut down!"))

(defn -main []
  (let [port (-> (slurp ".nrepl-port") (edn/read-string))]
    (log/info "Starting nREPL server on port" port)
    (nrepl/start-server :port port :handler (ns-resolve 'cider.nrepl 'cider-nrepl-handler)))

  (start!))
