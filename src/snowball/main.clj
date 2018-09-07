(ns snowball.main
  (:require [clojure.edn :as edn]
            [clojure.tools.nrepl.server :as nrepl]
            [cider.nrepl]
            [bounce.system :as b]
            [taoensso.timbre :as log]))

(defn -main []
  (let [port (-> (slurp ".nrepl-port") (edn/read-string))]
    (log/info "Starting nREPL server on port" port)
    (nrepl/start-server :port port :handler (ns-resolve 'cider.nrepl 'cider-nrepl-handler)))

  (log/info "Starting components...")
  (b/set-opts! #{'snowball.config/value
                 'snowball.discord/client
                 'snowball.watch/<presence
                 'snowball.watch/<five-queue
                 'snowball.speech/synthesiser})
  (b/start!)
  (log/info "Everything's up and running!"))
