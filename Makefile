.PHONY: run

run:
	export GOOGLE_APPLICATION_CREDENTIALS="$(shell pwd)/keys/google.json"; clojure -m snowball.main
