.PHONY: run outdated porcupine

run: porcupine
	GOOGLE_APPLICATION_CREDENTIALS="$(shell pwd)/resources/google.json" \
    LD_LIBRARY_PATH="wake-word-engine/Porcupine/lib/linux/x86_64" \
	clj # -m snowball.main

porcupine:
	if [ ! -d "wake-word-engine/Porcupine" ]; then \
		mkdir -p wake-word-engine; \
		cd wake-word-engine; \
		git clone git@github.com:Picovoice/Porcupine.git; \
		cd Porcupine/binding/android/porcupine/src/main/java/ai/picovoice/porcupine; \
		javac Porcupine.java PorcupineException.java; \
	fi
	if [ ! -d "wake-word-engine/hey snowball_linux.ppn" ]; then \
		cd wake-word-engine/Porcupine; \
		tools/optimizer/linux/x86_64/pv_porcupine_optimizer -r resources/ -w "hey snowball" -p linux -o ../; \
	fi

outdated:
	clj -Aoutdated
