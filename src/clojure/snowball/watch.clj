(ns snowball.watch
  (:require [clojure.core.async :as a]
            [bounce.system :as b]
            [taoensso.timbre :as log]
            [java-time :as jt]
            [snowball.config :as config]
            [snowball.discord :as discord]
            [snowball.speech :as speech]))

(b/defcomponent presence-chan {:bounce/deps #{discord/client config/value}}
  (log/info "Starting presence poller")
  (let [closed?! (atom false)]
    (-> (a/go-loop []
          (when-not @closed?!
            (a/<! (a/timeout (get-in config/value [:watch :poll-ms])))
            (let [desired-channel (->> (discord/channels)
                                       (filter discord/has-speaking-users?)
                                       (first))
                  current-channel (discord/current-channel)]
              (cond
                (and current-channel (nil? desired-channel))
                (discord/leave! current-channel)

                (and (or (nil? current-channel)
                         (not (discord/has-speaking-users? current-channel)))
                     desired-channel)
                (discord/join! desired-channel)))
            (recur)))
        (b/with-stop
          (log/info "Stopping presence poller")
          (reset! closed?! true)
          (a/close! presence-chan)))))

(b/defcomponent five-queue-chan {:bounce/deps #{discord/client config/value}}
  (log/info "Starting five queue poller")
  (let [closed?! (atom false)]
    (-> (a/go-loop [previous-size nil
                    last-announcement nil]
          (when-not @closed?!
            (a/<! (a/timeout (get-in config/value [:watch :poll-ms])))
            (if-let [channel (discord/current-channel)]
              (let [channel-size (->> (discord/channel-users channel)
                                      (filter discord/can-speak?)
                                      (count))
                    target 5]
                (recur channel-size
                       (if (and (= channel-size target)
                                (or (nil? previous-size) (< previous-size target))
                                (or (nil? last-announcement) (jt/after? (jt/instant) (jt/plus last-announcement (jt/hours 1)))))
                         (do
                           (speech/say! (str "That's the " target " queue!"))
                           (log/info "Announced the five queue")
                           (jt/instant))
                         last-announcement)))
              (recur nil nil))))
        (b/with-stop
          (log/info "Stopping five queue poller")
          (reset! closed?! true)
          (a/close! five-queue-chan)))))

(b/defcomponent pollers {:bounce/deps #{presence-chan five-queue-chan}}
  nil)
