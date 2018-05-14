(ns snowball.main
  (:require [clojure.tools.logging :as log]
            [clojure.tools.nrepl.server :as nrepl]
            [cider.nrepl]))

(defn start-nrepl! [port]
  (nrepl/start-server :port port :handler (ns-resolve 'cider.nrepl 'cider-nrepl-handler))
  (log/info "nREPL server started on" port))

(defn -main []
  (start-nrepl! 9001))
