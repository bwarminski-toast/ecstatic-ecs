FROM alpine:3.8

RUN apk add --no-cache curl bash openjdk8-jre
RUN mkdir /app
COPY target/hello-1.0-SNAPSHOT.jar /app
RUN mkdir /config
COPY config.yml /config

ENTRYPOINT ["java", "-jar", "/app/hello-1.0-SNAPSHOT.jar", "server", "/config/config.yml"]
