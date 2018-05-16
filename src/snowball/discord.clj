(ns snowball.discord
  (:require [taoensso.timbre :as log])
  (:import sx.blah.discord.api.ClientBuilder))

;; https://github.com/Discord4J/Discord4J
;; https://jitpack.io/com/github/Discord4J/Discord4J/2.10.1/javadoc/

(defonce client! (atom nil))

(defn connect! [{:keys [token]}]
  (log/info "Connecting to Discord")
  (->> (.. (ClientBuilder.)
           (withToken token)
           (login))
       (reset! client!)))
