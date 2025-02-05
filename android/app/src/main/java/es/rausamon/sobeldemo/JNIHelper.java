package es.rausamon.sobeldemo;

import org.opencv.core.Mat;

import android.graphics.Bitmap;

public class JNIHelper {
    static {
        System.loadLibrary("native-lib");
    }

    public static native void setCores(int newNumCores);

    public static void sobelFilter(Mat input, Mat output) {
        sobelFilter(input.getNativeObjAddr(), output.getNativeObjAddr());
    }

    public static Mat sobelFilterOnNewMat(Mat input) {
        return new Mat(sobelFilterOnNewMat(input.getNativeObjAddr()));
    }

    public static native void sobelFilter(long matAddrInput, long matAddrOutput);

    // Método nativo para aplicar el filtro Sobel y devolver un nuevo Mat
    public static native long sobelFilterOnNewMat(long matAddrInput);


    public static Mat bitmapToMat(Bitmap bitmap) {
        Mat mat = new Mat();
        org.opencv.android.Utils.bitmapToMat(bitmap, mat);
        return mat;
    }

    /**
     * Converts a CV Mat to an Android bitmap for visualization
     */
    public static Bitmap matToBitmap(Mat mat) {
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(mat, bitmap);
        return bitmap;
    }
}