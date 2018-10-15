(ns snowball.command
  (:require [clojure.string :as str]
            [clojure.core.async :as a]
            [bounce.system :as b]
            [taoensso.timbre :as log]
            [snowball.comprehension :as comprehension]
            [snowball.util :as util]
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
      (if-let [{:keys [channel user]} (get-in config/value [:command :music])]
        (do
          (log/info "Music command" command)
          (acknowledge!)

          (when (and user
                     (not= command "summon")
                     (every? #(not= user (discord/->id %)) (discord/channel-users (discord/current-channel))))
            (discord/send! channel "!summon"))

          (discord/send! channel (str "!" command)))
        (do
          (log/info "Tried to use a music bot command without {:command {:music {:channel ...}}} being set")
          (speech/say! "You need to set the command music channel setting if you want me to control the music bot"))))
    (catch Exception e
      (log/error "Error while executing music command" e))))

(defn handle-command! [{:keys [phrase user]}]
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

    #"(pause|stop)" :>>
    (fn [_]
      (music-command! "pause"))

    #"(resume|unpause)" :>>
    (fn [_]
      (music-command! "resume"))

    #"skip" :>>
    (fn [_]
      (music-command! "skip"))

    #"summon" :>>
    (fn [_]
      (music-command! "summon"))

    #"dismiss" :>>
    (fn [_]
      (music-command! "disconnect"))

    #"clear" :>>
    (fn [_]
      (music-command! "clear"))

    #".*volume.*" :>>
    (fn [command]
      (condp re-find command
        #"(increase|up|raise).*?(\d+)" :>>
        (fn [[_ _ amount]]
          (music-command! (str "volume +" amount)))

        #"(decrease|down|lower).*?(\d+)" :>>
        (fn [[_ _ amount]]
          (music-command! (str "volume -" amount)))

        #"(increase|up|raise)" :>>
        (fn [_]
          (music-command! "volume +15"))

        #"(decrease|down|lower)" :>>
        (fn [_]
          (music-command! "volume -15"))

        #"(\d+)" :>>
        (fn [[_ amount]]
          (music-command! (str "volume " amount)))

        (speech/say! "I need at least a number to set the volume.")))

    #".*move.*" :>>
    (fn [command]
      (letfn [(->name [x] (when x (-> x (cond-> (not (string? x)) discord/->name) util/sanitise-entity)))
              (->names-list [xs] (str/join ", " (map discord/->name xs)))
              (included [s targets] (filter #(str/includes? s (->name %)) targets))]
        (let [channels (discord/channels)
              users (into #{} (mapcat discord/channel-users) channels)
              target-channel (->> (included command (conj channels "this channel" "my channel" "our channel" "here"))
                                  (map (fn [x]
                                            (case x
                                              ("this channel" "my channel" "our channel" "here") (discord/current-channel)
                                              x)))
                                  (first))
              target-users (into #{}
                                 (mapcat (fn [x]
                                           (case x
                                             ("me" "myself") [user]
                                             "everyone" users
                                             [x])))
                                 (included command (conj users "me" "myself" "everyone")))]
          (if (and target-channel (seq target-users))
            (do
              (log/info "Moving" (->names-list target-users) "to" (->name target-channel))
              (doseq [user target-users]
                (discord/move-user-to-voice-channel user target-channel)))
            (do
              (log/info "Invalid move for users" (->names-list target-users) "to" (->name target-channel))
              (speech/say! "I need usernames and a channel name to do that."))))))

    #"(ignore|no|nevermind|fuck off|go away|get lost|fuck you)" :>>
    (fn [_]
      (log/info "Going back to sleep")
      (acknowledge!))

    (do
      (log/info "Couldn't find a matching command")
      (speech/say! "Sorry, I didn't recognise that command."))))

(b/defcomponent dispatcher {:bounce/deps #{comprehension/phrase-text-chan speech/synthesiser config/value}}
  (log/info "Starting command dispatcher loop")

  (swap! comprehension/extra-phrases! into
         #{"say" "play" "pause" "pause the music" "stop"
           "stop the music" "resume" "resume the music"
           "unpause" "unpause the music" "skip" "skip this song"
           "summon" "dismiss" "clear" "clear the queue" "reduce"
           "increase" "volume" "music" "up" "down" "move"
           "everyone" "me" "myself" "here" "this" "my" "our" "channel"})

  (a/go-loop []
    (when-let [{:keys [user phrase] :as command} (a/<! comprehension/phrase-text-chan)]
      (try
        (let [user-name (discord/->name user)]
          (log/info (str "Handling phrase from " user-name ": " phrase)))
        (handle-command! command)
        (catch Exception e
          (log/error "Caught error in dispatcher loop" e)))
      (recur))))
