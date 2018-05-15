(ns snowball.discord
  (:require [taoensso.timbre :as log])
  (:import sx.blah.discord.api.ClientBuilder))

;; https://github.com/Discord4J/Discord4J
;; https://jitpack.io/com/github/Discord4J/Discord4J/2.10.1/javadoc/

(defn connect! [{:keys [token]}]
  (log/info "Connecting to Discord")
  (let [client (ClientBuilder.)]
    (.withToken client token)
    (.login client)))
