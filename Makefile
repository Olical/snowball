.PHONY: run outdated porcupine

run: porcupine
	GOOGLE_APPLICATION_CREDENTIALS="$(shell pwd)/resources/google.json" \
	LD_LIBRARY_PATH="wake-word-engine/jni" \
	clj -e "(do (import '[snowball Porcupine]) (Porcupine. \"wake-word-engine/Porcupine/lib/common/porcupine_params.pv\" \"wake-word-engine/hey snowball_li.ppm\" 0.5))"
	# -m snowball.main

porcupine:
	if [ ! -d "wake-word-engine/Porcupine" ]; then \
		mkdir -p wake-word-engine; \
		cd wake-word-engine; \
		git clone git@github.com:Picovoice/Porcupine.git; \
	fi
	if [ ! -d "wake-word-engine/hey snowball_linux.ppn" ]; then \
		cd wake-word-engine/Porcupine; \
		tools/optimizer/linux/x86_64/pv_porcupine_optimizer -r resources/ -w "hey snowball" -p linux -o ../; \
	fi
	if [ ! -f "wake-word-engine/jni" ]; then \
		mkdir -p wake-word-engine/jni; \
		javac src/java/snowball/Porcupine.java; \
		javah -cp src/java -o wake-word-engine/jni/porcupine.h snowball.Porcupine; \
		gcc -shared -O3 -fPIC \
		    -I/usr/include \
		    -I/usr/lib/jvm/default/include \
		    -I/usr/lib/jvm/default/include/linux \
		    ./wake-word-engine/Porcupine/lib/linux/x86_64/libpv_porcupine.a \
		    ./wake-word-engine/jni/porcupine.h \
		    -o wake-word-engine/jni/libpv_porcupine.so; \
	fi

outdated:
	clj -Aoutdated
