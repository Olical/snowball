(ns snowball.main
  (:require [taoensso.timbre :as log]
            [snowball.config :as config]
            [snowball.nrepl :as nrepl]
            [snowball.discord :as discord]
            [snowball.watch :as watch]
            [snowball.speech :as speech]))

(defn -main []
  (config/init!)
  (nrepl/init!)
  (discord/init!)
  (watch/init!)
  (speech/init!)
  (log/info "Everything's up and running!"))
