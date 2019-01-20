#!/usr/bin/env bash

GOOGLE_APPLICATION_CREDENTIALS="$(pwd)/config/google.json" \
LD_LIBRARY_PATH="wake-word-engine/jni" \
clojure -J-Dclojure.server.snowball="{:port 5005 :accept clojure.core.server/io-prepl}" \
        -m snowball.main 
