FROM ubuntu:18.04

RUN mkdir /app

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
        wget \
        unzip \
        curl \
        ca-certificates \
        default-jre \
        && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

RUN url=$(curl --silent "https://api.github.com/repos/Waikato/moa/releases/latest"  | grep -Po '"browser_download_url": "\K.*-bin\.zip(?=")') && \
    wget $url && \
    file=$(echo $url | grep -Po '/[0-9.]{1,}\/\K.*[^-bin.zip]') && \
    unzip $file-bin.zip && mv $file moa && \
    rm $file-bin.zip

CMD moa/bin/moa.sh
