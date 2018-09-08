.PHONY: run outdated porcupine

run: porcupine
	export GOOGLE_APPLICATION_CREDENTIALS="$(shell pwd)/resources/google.json"; clojure -m snowball.main

porcupine:
	if [ -d "wake-word-engine/Porcupine" ]; then \
		cd wake-word-engine/Porcupine; \
		git pull; \
	else \
		mkdir -p wake-word-engine; \
		cd wake-word-engine; \
		git clone git@github.com:Picovoice/Porcupine.git; \
	fi
	if [ ! -d "wake-word-engine/hey snowball_linux.ppn" ]; then \
		cd wake-word-engine/Porcupine; \
		tools/optimizer/linux/x86_64/pv_porcupine_optimizer -r resources/ -w "hey snowball" -p linux -o ../; \
	fi

outdated:
	clj -Aoutdated
