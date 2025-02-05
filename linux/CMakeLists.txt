cmake_minimum_required(VERSION 3.10)

project(SobelGui)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fopenmp")

include_directories(${CMAKE_SOURCE_DIR}/../common)

find_package(OpenCV REQUIRED)
find_package(GTest REQUIRED)

set(SOURCES
    sobelgui.cpp
    ../common/sobel.cpp
)

add_executable(sobelgui ${SOURCES})

target_link_libraries(sobelgui ${OpenCV_LIBS})

enable_testing()

set(TEST_SOURCES
    test_sobelgui.cpp
)

add_executable(runTests ${TEST_SOURCES})

target_link_libraries(runTests GTest::GTest GTest::Main ${OpenCV_LIBS} pthread)

add_test(NAME SobelGuiTest COMMAND runTests)
