(ns snowball.discord
  (:require [taoensso.timbre :as log]))

(defn connect! [{:keys []}]
  (log/info "Connecting to Discord"))
