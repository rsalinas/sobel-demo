#! /bin/bash
set -eu

cd "$(dirname "$0")"
! which bsdtar >/dev/null &&
	sudo apt install libarchive-tools
mkdir opencv.tmp
curl -L https://github.com/opencv/opencv/releases/download/4.11.0/opencv-4.11.0-android-sdk.zip | bsdtar -xf - --strip-components=2 -C opencv.tmp OpenCV-android-sdk/sdk
mv opencv.tmp opencv
