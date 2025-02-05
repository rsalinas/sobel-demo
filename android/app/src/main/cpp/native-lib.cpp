#include "sobel.h"

#include <android/log.h>
#include <jni.h>

#define APPNAME "SobelDemo"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, APPNAME, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, APPNAME, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, APPNAME, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, APPNAME, __VA_ARGS__)

extern "C" JNIEXPORT void JNICALL
Java_es_rausamon_sobeldemo_JNIHelper_setCores(JNIEnv *env, jobject instance, jint setThreads)
{
    LOGD("Setting number of cores to %d", setThreads);
    sobelSetThreads(setThreads);
}

/**
 * Filters the given image onto the already existing Mat object.
 */
extern "C" JNIEXPORT void JNICALL
Java_es_rausamon_sobeldemo_JNIHelper_sobelFilter(JNIEnv *env, jobject instance, jlong matAddrInput, jlong matAddrOutput)
{
    cv::Mat &input = *(cv::Mat *)matAddrInput;
    cv::Mat &output = *(cv::Mat *)matAddrOutput;
    sobelFilter(input, output);
}

/**
 * Instantiates a new Mat() object, which is filled with the filtered image.
 *
 * This looks more functional but involves the creation of more Java objects, which is relatively expensive.
 */
extern "C" JNIEXPORT jlong JNICALL
Java_es_rausamon_sobeldemo_JNIHelper_sobelFilterOnNewMat(JNIEnv *env, jobject instance, jlong matAddrInput)
{
    cv::Mat &input = *(cv::Mat *)matAddrInput;
    cv::Mat *output = new cv::Mat();
    Java_es_rausamon_sobeldemo_JNIHelper_sobelFilter(env, instance, matAddrInput, (jlong)output);
    return (jlong)output;
}