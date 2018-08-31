.PHONY: run outdated

run:
	export GOOGLE_APPLICATION_CREDENTIALS="$(shell pwd)/keys/google.json"; clojure -m snowball.main

outdated:
	clj -Aoutdated
