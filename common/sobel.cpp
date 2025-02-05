

#include "sobel.h"

#include <omp.h>

static int numThreads = 1;

/**
 * @brief Sets the number of threads to be used in the Sobel filter operation.
 *
 * This function configures the number of threads that will be utilized
 * when applying the Sobel filter to an image. It affects the parallelization
 * of the filtering process.
 *
 * @param n The number of threads to be used. Should be a positive integer.
 *
 * @return void This function does not return a value.
 */
void sobelSetThreads(int n)
{
    if (numThreads > 0)
    {
        numThreads = n;
    }
}

void sobelFilter(const cv::Mat &input, cv::Mat &output)
{
    // Sobel operator kernels
    static const std::vector<int> gx = {-1, 0, 1, -2, 0, 2, -1, 0, 1};
    static const std::vector<int> gy = {-1, -2, -1, 0, 0, 0, 1, 2, 1};

    // Adjust the target image size
    output.create(input.size(), CV_8U);
    if (output.empty())
    {
        // TODO handle memory allocation error. What could we do here?
        return;
    }

#pragma omp parallel for num_threads(numThreads)
    for (int y = 1; y < input.rows - 1; y++)
    {
        for (int x = 1; x < input.cols - 1; x++)
        {
            int sumX = 0;
            int sumY = 0;

            for (int ky = -1; ky <= 1; ky++)
            {
                for (int kx = -1; kx <= 1; kx++)
                {
                    int pixel = input.at<uchar>(y + ky, x + kx);
                    sumX += pixel * gx[(ky + 1) * 3 + (kx + 1)];
                    sumY += pixel * gy[(ky + 1) * 3 + (kx + 1)];
                }
            }
            
            // Using Euclidean distance to calculate the magnitude
            int magnitude = std::sqrt(sumX * sumX + sumY * sumY);
            output.at<uchar>(y, x) = std::min(255, magnitude);
        }
    }
}
