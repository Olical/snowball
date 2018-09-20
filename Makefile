.PHONY: default run run-container build push outdated

NAME := olical/snowball
TAG := $$(git log -1 --pretty=%H)
IMG := ${NAME}:${TAG}
LATEST := ${NAME}:latest

default: wake-word-engine run

run:
	GOOGLE_APPLICATION_CREDENTIALS="$(shell pwd)/config/google.json" \
	LD_LIBRARY_PATH="wake-word-engine/jni" \
	clojure -m snowball.main

run-container:
	docker run -p 9045:9045 -v $(shell pwd)/config:/usr/snowball/config -ti --rm olical/snowball

build: wake-word-engine
	docker build -t ${IMG} .
	docker tag ${IMG} ${LATEST}

push:
	docker push ${NAME}

outdated:
	clojure -Aoutdated

wake-word-engine: wake-word-engine/Porcupine wake-word-engine/hey_snowball_linux.ppn wake-word-engine/jni/libpv_porcupine.so

wake-word-engine/Porcupine:
	mkdir -p wake-word-engine
	cd wake-word-engine && git clone git@github.com:Picovoice/Porcupine.git

wake-word-engine/hey_snowball_linux.ppn: wake-word-engine/Porcupine
	cd wake-word-engine/Porcupine && tools/optimizer/linux/x86_64/pv_porcupine_optimizer -r resources/ -w "hey snowball" -p linux -o ../
	mv "wake-word-engine/hey snowball_linux.ppn" wake-word-engine/hey_snowball_linux.ppn

wake-word-engine/jni/snowball_porcupine_Porcupine.h: src/java/snowball/porcupine/Porcupine.java
	mkdir -p wake-word-engine/jni
	javac -h wake-word-engine/jni src/java/snowball/porcupine/Porcupine.java

wake-word-engine/jni/libpv_porcupine.so: wake-word-engine/jni/snowball_porcupine_Porcupine.h src/c/porcupine.c
	gcc -shared -O3 \
		-I/usr/include \
		-I/usr/lib/jvm/default/include \
		-I/usr/lib/jvm/default/include/linux \
		-Iwake-word-engine/Porcupine/include \
		-Iwake-word-engine/jni \
		src/c/porcupine.c \
		wake-word-engine/Porcupine/lib/linux/x86_64/libpv_porcupine.a \
		-o wake-word-engine/jni/libpv_porcupine.so
