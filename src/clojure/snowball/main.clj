(ns snowball.main
  (:require [bounce.system :as b]
            [taoensso.timbre :as log]))

(defn -main []
  (log/info "Starting components...")
  (b/set-opts! #{'snowball.config/value
                 'snowball.discord/audio-chan
                 'snowball.comprehension/phrase-text-chan
                 'snowball.speech/synthesiser
                 'snowball.presence/poller
                 'snowball.command/dispatcher})
  (b/start!)
  (log/info "Everything's up and running!"))
