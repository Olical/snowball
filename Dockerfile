FROM clojure:tools-deps-1.9.0.394

RUN mkdir -p /usr/snowball
WORKDIR /usr/snowball

COPY deps.edn /usr/snowball
RUN clojure -e :ready

COPY . /usr/snowball

CMD ["./run.sh"]
