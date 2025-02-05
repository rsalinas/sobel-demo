#include <gtest/gtest.h>
#include <cstdlib>
#include <string>
#include <cstdio>
#include <unistd.h>
#include <opencv2/opencv.hpp>

const std::string testImagePath = "../testdata/lena.jpg";
const std::string outputFilename = "output.jpg";

/**
 * @brief Test class for SobelGui.
 *
 * This class contains setup and teardown methods for the SobelGui application tests.
 */
class SobelGuiTest : public ::testing::Test
{
protected:
    /**
     * @brief Setup method for tests.
     *
     * Creates a symbolic link from /dev/full to full.jpg.
     */
    void SetUp() override
    {
        if (symlink("/dev/full", "full.jpg") != 0)
        {
            perror("symlink");
            FAIL() << "Failed to create symlink";
        }
    }

    /**
     * @brief Teardown method for tests.
     *
     * Removes the symbolic link created in SetUp.
     */
    void TearDown() override
    {
        if (unlink("full.jpg") != 0)
        {
            perror("unlink");
        }
    }
};

/**
 * @brief Test that verifies error handling when writing to /dev/full.
 */
TEST_F(SobelGuiTest, FullDeviceTest)
{
    int result = std::system(("./sobelgui -i=" + testImagePath + " -o=full.jpg").c_str());
    ASSERT_NE(result, EXIT_SUCCESS);
}

/**
 * @brief Test that verifies the help option.
 *
 * Runs the application with the --help parameter and checks that the return value is zero.
 */
TEST_F(SobelGuiTest, HelpOption)
{
    int result = std::system("./sobelgui --help > /dev/null");
    ASSERT_EQ(result, EXIT_SUCCESS);
}

/**
 * @brief Test that verifies the creation of an output file.
 *
 * Runs the application with parameters -i=" + testImagePath + " -o=" + outputFilename and checks that the output file exists and is in JPG format.
 */
TEST_F(SobelGuiTest, OutputFileTest)
{
    int result = std::system(("./sobelgui -i=" + testImagePath + " -o=" + outputFilename).c_str());
    ASSERT_EQ(result, EXIT_SUCCESS);

    ASSERT_EQ(access(outputFilename.c_str(), F_OK), 0) << "Missing output file";

    cv::Mat img = cv::imread(outputFilename, cv::IMREAD_COLOR);
    ASSERT_FALSE(img.empty()) << "The output file seems corrupted";

    cv::Mat testImg = cv::imread(testImagePath, cv::IMREAD_COLOR);
    ASSERT_EQ(testImg.size(), img.size()) << "The output file has different size than the input file";
}

/**
 * @brief Main function to run all tests.
 *
 * Initializes GoogleTest and runs all defined tests.
 *
 * @param argc Number of command-line arguments.
 * @param argv Array of command-line arguments.
 * @return int Return code from the tests.
 */
int main(int argc, char **argv)
{
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}