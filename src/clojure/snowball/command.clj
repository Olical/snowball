(ns snowball.command
  (:require [clojure.string :as str]
            [clojure.core.async :as a]
            [bounce.system :as b]
            [taoensso.timbre :as log]
            [snowball.comprehension :as comprehension]
            [snowball.discord :as discord]
            [snowball.speech :as speech]
            [snowball.config :as config]))

(defn music-channel []
  (if-let [channel (get-in config/value [:command :music-channel])]
    channel
    (do
      (log/info "Tried to use a Rythm bot command without {:command {:music-channel ...}} being set")
      (speech/say! "You need to set the command music-channel setting if you want me to control Rythm bot.")
      nil)))

(defn handle-command! [{:keys [phrase]}]
  (condp re-find phrase

    #"say (.*)" :>>
    (fn [[_ content]]
      (log/info "Saying:" content)
      (speech/say! content))

    #"who's a good boy" :>>
    (fn [_]
      (log/info "Acknowledging that I'm a good boy")
      (speech/say! "I'm a good boy! It's me! I'm the good boy! Woof!"))

    #"play (.*)" :>>
    (fn [[_ song]]
      (log/info "Playing" song)
      (when-let [music-channel (music-channel)]
        (speech/say! (str "Playing " song))
        (discord/send! music-channel (str "!play " song))))

    #"(pause|stop)" :>>
    (fn [_]
      (log/info "Pausing")
      (when-let [music-channel (music-channel)]
        (speech/say! "Pausing")
        (discord/send! music-channel (str "!pause"))))

    #"resume" :>>
    (fn [_]
      (log/info "Resuming")
      (when-let [music-channel (music-channel)]
        (speech/say! "Resuming")
        (discord/send! music-channel (str "!resume"))))

    #"skip" :>>
    (fn [_]
      (log/info "Skipping")
      (when-let [music-channel (music-channel)]
        (speech/say! "Skipping")
        (discord/send! music-channel (str "!skip"))))

    (do
      (log/info "Couldn't find a matching command")
      (speech/say! "Sorry, I didn't recognise that command."))))

(b/defcomponent dispatcher {:bounce/deps #{comprehension/phrase-text-chan speech/synthesiser config/value}}
  (log/info "Starting command dispatcher loop")
  (a/go-loop []
    (when-let [{:keys [user phrase] :as command} (a/<! comprehension/phrase-text-chan)]
      (try
        (let [user-name (discord/->name user)]
          (log/info (str "Handling phrase from " user-name ": " phrase)))
        (handle-command! command)
        (catch Error e
          (log/error "Caught error in dispatcher loop" e)))
      (recur))))

