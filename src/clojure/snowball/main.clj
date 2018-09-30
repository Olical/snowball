(ns snowball.main
  (:require [clojure.core.server :as server]
            [clojure.edn :as edn]
            [bounce.system :as b]
            [taoensso.timbre :as log]
            [snowball.config :as config]))

(defn -main []
  (log/info "Starting components...")
  (b/set-opts! #{'snowball.config/value
                 'snowball.discord/audio-chan
                 'snowball.comprehension/phrase-text-chan
                 'snowball.speech/synthesiser
                 'snowball.presence/poller
                 'snowball.command/dispatcher})
  (b/start!)

  (let [port (get-in config/value [:system :socket-repl-port])]
    (log/info "Starting socket REPL on port" port)
    (server/start-server {:name "system"
                          :port port
                          :accept 'clojure.core.server/repl}))

  (log/info "Everything's up and running!"))
