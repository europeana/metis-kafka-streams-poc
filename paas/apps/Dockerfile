#REQUIREES TO COPY THE JAR INTO APPS PAAS/APPS DIRECTORY
FROM bitnami/minideb:latest

USER 0

ENV JAVA_HOME=/opt/java/openjdk
COPY --from=eclipse-temurin:21 $JAVA_HOME $JAVA_HOME
ENV PATH="${JAVA_HOME}/bin:${PATH}"

RUN apt-get update && apt-get install -y \
    xz-utils \
    telnet  \
    iputils-ping    \
    procps \
    traceroute \
    curl \
    wget \
    net-tools \
    dnsutils \
    vim

## Needed for media
## For optimalization we should create separate image for media so not all topology contain imagemagick

COPY install.sh /
RUN chmod =555 /install.sh
RUN /install.sh

### END of needed for media

USER 1001

COPY ./KafkaTopologies-1.0-SNAPSHOT-jar-with-dependencies.jar kafka-app.jar