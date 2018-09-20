(ns snowball.command
  (:require [clojure.string :as str]
            [clojure.core.async :as a]
            [bounce.system :as b]
            [taoensso.timbre :as log]
            [snowball.comprehension :as comprehension]
            [snowball.discord :as discord]
            [snowball.speech :as speech]))

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

    (do
      (log/info "Couldn't find a matching command")
      (speech/say! "Sorry, I didn't recognise that command."))))

(b/defcomponent dispatcher {:bounce/deps #{comprehension/phrase-text-chan speech/synthesiser}}
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

