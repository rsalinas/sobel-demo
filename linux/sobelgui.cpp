#include "sobel.h"
#include <opencv2/opencv.hpp>

#include <chrono>
#include <iostream>
#include <string>
#include <functional>

constexpr auto kCameraWindowTitle = "Source";
constexpr auto kFilteredWindowTitle = "Sobel";

/**
 * @brief Custom exception class for camera errors.
 */
class CameraError : public std::runtime_error
{
public:
    explicit CameraError(const std::string &what) : std::runtime_error(what) {}
};

/**
 * @brief Processes an input file and saves the result to an output file.
 *
 * @param input_file Path to the input file.
 * @param output_file Path to the output file.
 * @return int 0 on success, 1 on failure.
 */
static int process_file(const std::string &input_file, const std::string &output_file);

/**
 * @brief Class for applying Sobel filter to camera feed and displaying the result.
 */
class SobelCameraFilterGui
{
public:
    SobelCameraFilterGui();
    ~SobelCameraFilterGui();

    bool InitCamera(int camera_id = 0);
    void VideoLoop(int run_time = 0);

    bool HandleKey(int key);

private:
    cv::VideoCapture cap_;
    cv::Mat sobel_result_;
    int frame_count_ = 0;
    std::chrono::high_resolution_clock::time_point start_time_;

    bool show_cam_ = false;
    bool show_filtered_ = true;
    int threads_ = 1;
};

SobelCameraFilterGui::SobelCameraFilterGui() = default;

SobelCameraFilterGui::~SobelCameraFilterGui()
{
    cap_.release();
    cv::destroyAllWindows();
}

bool SobelCameraFilterGui::InitCamera(int camera_id)
{
    cap_.open(camera_id);
    if (!cap_.isOpened())
    {
        std::cerr << "Error opening camera." << std::endl;
        return false;
    }
    cap_.set(cv::CAP_PROP_FORMAT, CV_8UC1);
    cap_.set(cv::CAP_PROP_FRAME_WIDTH, 1920);
    cap_.set(cv::CAP_PROP_FRAME_HEIGHT, 1080);

    start_time_ = std::chrono::high_resolution_clock::now();
    return true;
}

/**
 * @brief Measures the execution time of a function.
 *
 * @param func The function to measure.
 * @return double The execution time in milliseconds.
 */
double measureExecutionTime(std::function<void()> func)
{
    auto start = std::chrono::high_resolution_clock::now();
    func();
    auto stop = std::chrono::high_resolution_clock::now();
    std::chrono::duration<double, std::milli> duration = stop - start;
    // std::clog << "Cost: " << duration.count() << " ms" << std::endl;
    return duration.count();
}

/**
 * @brief Calculates and displays the frames per second (FPS).
 *
 * @param frame_count The number of frames processed.
 * @param start_time The start time of the measurement period.
 */
void calculateAndDisplayFPS(int &frame_count, std::chrono::high_resolution_clock::time_point &start_time)
{
    frame_count++;
    auto current_time = std::chrono::high_resolution_clock::now();
    auto elapsed_time = std::chrono::duration_cast<std::chrono::milliseconds>(current_time - start_time).count();

    if (elapsed_time >= 1000)
    {
        double fps = frame_count / (elapsed_time / 1000.0);
        std::cout << "FPS: " << fps << std::endl;
        frame_count = 0;
        start_time = current_time;
    }
}

void SobelCameraFilterGui::VideoLoop(int run_time)
{
    cv::Mat frame, gray;
    fprintf(stderr, "Entering video loop. Exit with 'q'.\n");
    auto loop_start_time = std::chrono::high_resolution_clock::now();
    while (true)
    {
        cap_ >> frame; // Capture a frame from the camera
        if (frame.empty())
        {
            std::cerr << "Cannot capture frame." << std::endl;
            return;
        }

        // Show unprocessed image if enabled.
        if (show_cam_)
        {
            cv::imshow(kCameraWindowTitle, frame);
        }

        cv::cvtColor(frame, gray, cv::COLOR_BGR2GRAY);

        auto filter = [&]()
        {
            sobelFilter(gray, sobel_result_);
        };
        measureExecutionTime(filter);

        if (show_filtered_)
        {
            cv::imshow(kFilteredWindowTitle, sobel_result_);
        }

        calculateAndDisplayFPS(frame_count_, start_time_);

        int key = cv::waitKey(1);
        if (HandleKey(key))
        {
            // Exit key was pressed.
            break;
        }

        if (run_time > 0)
        {
            auto current_time = std::chrono::high_resolution_clock::now();
            auto elapsed_time = std::chrono::duration_cast<std::chrono::seconds>(current_time - loop_start_time).count();
            if (elapsed_time >= run_time)
            {
                std::cout << "Run time of " << run_time << " seconds reached. Exiting." << std::endl;
                break;
            }
        }
    }
}

int process_file(const std::string &input_file, const std::string &output_file)
{
    cv::Mat gray = cv::imread(input_file, cv::IMREAD_GRAYSCALE);
    if (gray.empty())
    {
        std::cerr << "Error opening input file: " << input_file << std::endl;
        return 1;
    }

    cv::Mat sobel_result;
    sobelFilter(gray, sobel_result);

    if (!output_file.empty())
    {
        // Save to file
        if (!cv::imwrite(output_file, sobel_result))
        {
            std::cerr << "Error saving image: " << output_file << std::endl;
            return 1;
        }
        std::cerr << "Sobel image saved as: " << output_file << std::endl;
    }
    else
    {
        // Show on screen
        cv::imshow(kFilteredWindowTitle, sobel_result);

        std::cerr << "Press any key to exit." << std::endl;
        cv::waitKey(0);
    }
    return 0;
}

bool SobelCameraFilterGui::HandleKey(int key)
{
    switch (key)
    {
    case 's':
        if (cv::imwrite("sobel.png", sobel_result_))
        {
            std::cout << "Sobel image saved as sobel.png" << std::endl;
        }
        else
        {
            std::cout << "Error saving image" << std::endl;
        }
        break;
    case 'c':
        show_cam_ = !show_cam_;
        if (!show_cam_)
        {
            cv::destroyWindow(kCameraWindowTitle);
        }
        break;
    case 'f':
        show_filtered_ = !show_filtered_;
        // Skip destruction of this window, we'll just pause it or will lose keyboard control.

        break;
    case 'q':
        // Signal a graceful closing has been requested.
        return true;
    default:
        if (isdigit(key))
        {
            int threads = key - '0';
            std::clog << "Change to " << threads << std::endl;
            sobelSetThreads(threads);
        }
        break;
    }
    return false;
}

int main(int argc, char **argv)
{
    constexpr auto keys =
        "{help h usage ? |      | print this message   }"
        "{i input        |      | set input file (optional) }"
        "{o output       |      | set output file (optional). If not given, resulting image is shown. }"
        "{c camera       |      | read from camera     }"
        "{t threads      |1     | number of threads    }"
        "{T run-time     |0     | run time in seconds (optional) }";

    cv::CommandLineParser parser(argc, argv, keys);
    parser.about("Sobel Demo v1.0");

    if (parser.has("help"))
    {
        parser.printMessage();
        return 0;
    }

    if (!parser.check())
    {
        parser.printErrors();
        return 1;
    }

    int threads = parser.get<int>("threads");
    std::cerr << "Using " << threads << " OpenMP threads" << std::endl;
    sobelSetThreads(threads);

    int run_time = parser.get<int>("run-time");

    if (parser.has("camera"))
    {
        SobelCameraFilterGui app;
        if (!app.InitCamera())
        {
            return 1;
        }
        app.VideoLoop(run_time);
    }
    else if (parser.has("input"))
    {
        std::string input_file = parser.get<std::string>("input");
        std::string output_file = parser.get<std::string>("output");
        return process_file(input_file, output_file);
    }
    else
    {
        std::cerr << "No input source specified. Use --camera or -i=<input file>.\n"
                  << std::endl;
        parser.printMessage();
        return 1;
    }
}
