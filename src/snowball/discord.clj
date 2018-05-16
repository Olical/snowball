(ns snowball.discord
  (:require [taoensso.timbre :as log])
  (:import sx.blah.discord.api.ClientBuilder))

;; https://github.com/Discord4J/Discord4J
;; https://jitpack.io/com/github/Discord4J/Discord4J/2.10.1/javadoc/

(def check-delay-ms 1000)
(defonce client! (atom nil))

(defn connect! [{:keys [token]}]
  (log/info "Connecting to Discord")
  (->> (.. (ClientBuilder.)
           (withToken token)
           (login))
       (reset! client!))
  (log/info "Connected, waiting until ready")
  (loop []
    (if (.isReady @client!)
      (log/info "Ready")
      (do
        (log/info "Not ready, sleeping for 1s")
        (Thread/sleep check-delay-ms)
        (recur)))))

(defn channels! []
  (seq (.getVoiceChannels @client!)))

(defn channel-users! [channel]
  (remove #(.isBot %) (seq (.getConnectedUsers channel))))

(defn current-channel! []
  (-> (.getConnectedVoiceChannels @client!)
      (seq)
      (first)))

(defn leave! [channel]
  (log/info "Joining" (.getName channel))
  (.leave channel))

(defn join! [channel]
  (log/info "Leaving" (.getName channel))
  (.join channel))
