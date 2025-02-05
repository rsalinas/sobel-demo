#!/bin/bash
set -eu

SRC_DIR=linux
BUILD_DIR=build

cmake -S ${SRC_DIR} -B ${BUILD_DIR}   # Configure cmake
cmake --build ${BUILD_DIR} -j$(nproc) # Build

ctest --output-on-failure \
    --verbose --test-dir ${BUILD_DIR} # Test
