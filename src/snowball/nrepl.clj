(ns snowball.nrepl
  (:require [clojure.tools.nrepl.server :as nrepl]
            [taoensso.timbre :as log]
            [cider.nrepl]))

(defn start! [{:keys [port]}]
  (log/info "Starting nREPL server on port" port)
  (nrepl/start-server :port port :handler (ns-resolve 'cider.nrepl 'cider-nrepl-handler)))
