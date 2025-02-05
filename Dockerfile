FROM ubuntu:24.04

RUN apt-get update && apt-get install -y \
    build-essential \
    cmake \
    libopencv-dev \
    libgtest-dev \
    git \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY common  common/
COPY linux linux/ 
COPY testdata testdata/ 
COPY build.sh .

RUN ./build.sh

ENTRYPOINT ["./build/sobelgui"]