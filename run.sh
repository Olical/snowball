#!/usr/bin/env bash

GOOGLE_APPLICATION_CREDENTIALS="$(pwd)/config/google.json" \
LD_LIBRARY_PATH="wake-word-engine/jni" \
clojure -m snowball.main
