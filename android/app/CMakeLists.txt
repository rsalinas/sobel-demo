cmake_minimum_required(VERSION 3.4.1)
project(SobelDemo)

set(OpenCV_DIR ${CMAKE_SOURCE_DIR}/../opencv/native)
set(DEMO_COMMON_DIR ${CMAKE_SOURCE_DIR}/../../common)

include_directories(${OpenCV_DIR}/jni/include)
include_directories(${DEMO_COMMON_DIR})

add_library(native-lib SHARED src/main/cpp/native-lib.cpp ${DEMO_COMMON_DIR}/sobel.cpp)

find_package(OpenMP REQUIRED)
find_library(jnigraphics-lib jnigraphics)
find_library(log-lib log)

set(OpenCV_LIB_PATH "${OpenCV_DIR}/libs/${ANDROID_ABI}/libopencv_java4.so")

message(STATUS "OpenCV_DIR: ${OpenCV_DIR}")
message(STATUS "OpenCV_LIB_PATH: ${OpenCV_LIB_PATH}")
message(STATUS "ANDROID_ABI: ${ANDROID_ABI}")
message(STATUS "Includedir: ${OpenCV_DIR}/../include")

if(NOT EXISTS ${OpenCV_LIB_PATH})
    message(FATAL_ERROR "Unsupported ABI or missing library: ${ANDROID_ABI}")
endif()

target_link_libraries(native-lib ${log-lib} OpenMP::OpenMP_CXX ${jnigraphics-lib} ${OpenCV_LIB_PATH})