FROM clojure:tools-deps-1.9.0.394-alpine
RUN apk add --no-cache make

RUN mkdir -p /usr/snowball
WORKDIR /usr/snowball

COPY deps.edn /usr/snowball
RUN clojure -e :ready

COPY . /usr/snowball

CMD ["make", "run"]
