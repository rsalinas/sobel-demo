/*
 * Sobel implementation that uses an OpenCV::Mat API.
 */

#ifndef SOBELDEMO_SOBEL_H
#define SOBELDEMO_SOBEL_H

#include <opencv2/opencv.hpp>

void sobelSetThreads(int n);
void sobelFilter(const cv::Mat &input, cv::Mat &output);

#endif //SOBELDEMO_SOBEL_H
