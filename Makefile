.PHONY: run outdated porcupine

run: porcupine
	GOOGLE_APPLICATION_CREDENTIALS="$(shell pwd)/resources/google.json" \
	LD_LIBRARY_PATH="lib" \
	clj # -m snowball.main

porcupine: porcupine-jni
	if [ ! -d "wake-word-engine/Porcupine" ]; then \
		mkdir -p wake-word-engine; \
		cd wake-word-engine; \
		git clone git@github.com:Picovoice/Porcupine.git; \
	fi
	if [ ! -d "wake-word-engine/hey snowball_linux.ppn" ]; then \
		cd wake-word-engine/Porcupine; \
		tools/optimizer/linux/x86_64/pv_porcupine_optimizer -r resources/ -w "hey snowball" -p linux -o ../; \
	fi

porcupine-jni:
	if [ ! -f "lib/libpv_porcupine.so" ]; then \
		mkdir -p lib; \
		javac src/java/snowball/Porcupine.java; \
		javah -cp src/java -o lib/porcupine.h snowball.Porcupine; \
		gcc -shared -O3 \
		    -I/usr/include \
		    -I/usr/lib/jvm/default/include \
		    -I/usr/lib/jvm/default/include/linux \
		    ./wake-word-engine/Porcupine/lib/linux/x86_64/libpv_porcupine.a \
		    ./lib/porcupine.h \
		    -o lib/libpv_porcupine.so; \
	fi

outdated:
	clj -Aoutdated
