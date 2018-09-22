(ns snowball.command
  (:require [clojure.string :as str]
            [clojure.core.async :as a]
            [bounce.system :as b]
            [taoensso.timbre :as log]
            [snowball.comprehension :as comprehension]
            [snowball.discord :as discord]
            [snowball.speech :as speech]
            [snowball.config :as config]))

(defn acknowledge! []
  (speech/say!
    (rand-nth
      ["You got it!" "Okay." "Sure thing." "Got it."
       "Yep." "Yes." "Sure." "Cool." "You're the boss."
       "Aight." "Yarp." "Anything for my princess."])))

(defn music-command! [command]
  (try
  (future
    (if-let [music-channel (get-in config/value [:command :music-channel])]
      (do
        (log/info "Music command" command)
        (acknowledge!)
        (discord/send! music-channel (str "!!!" command)))
      (do
        (log/info "Tried to use a music bot command without {:command {:music-channel ...}} being set")
        (speech/say! "You need to set the command music-channel setting if you want me to control the music bot"))))
  (catch Error e
    (log/error "Error while executing music command"))))

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
      (music-command! (str "play " song)))

    #"(pause|stop|resume)" :>>
    (fn [_]
      (music-command! "pause"))

    #"skip" :>>
    (fn [_]
      (music-command! "skip"))

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
