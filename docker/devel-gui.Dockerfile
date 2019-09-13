FROM ubuntu:18.04

RUN mkdir /app

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
        wget \
        git \
        ca-certificates \
        default-jre \
        openjdk-8-jdk \
        maven \
        unzip \
        && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*


RUN git clone https://github.com/Waikato/moa.git

RUN cd moa && \
    export JAVA_HOME="/usr" && \
    mvn clean install -DskipTests=true && \
    mvn -f release.xml package -DskipTests=true

RUN cd /app/moa/target/ && ls -1 | egrep 'moa-.*-SNAPSHOT-bin.zip' | xargs cp -t /app

RUN rm -rf moa

RUN ls -1 | egrep 'moa-.*-SNAPSHOT-bin.zip' | xargs unzip

RUN rm *.zip

RUN mv moa-* moa

CMD moa/bin/moa.sh
