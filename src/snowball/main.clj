(ns snowball.main
  (:require [taoensso.timbre :as log]
            [snowball.config :as config]
            [snowball.nrepl :as nrepl]
            [snowball.discord :as discord]
            [snowball.watch :as watch]
            [snowball.speech :as speech]
            [snowball.comprehension :as comprehension]))

(defn -main []
  (config/init!)
  (nrepl/init!)
  (discord/init!)
  (watch/init!)
  (speech/init!)

  ;; TODO Turn back on when I have working audio streams.
  ; (comprehension/init!)
  (log/info "Everything's up and running!"))
