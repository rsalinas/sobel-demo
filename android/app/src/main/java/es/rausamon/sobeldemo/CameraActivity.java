/**
 * Activity for realtime filtering of the camera images.
 */

package es.rausamon.sobeldemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "SobelDemo";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private PreviewView previewView;
    private ImageView imageViewFiltered;
    private TextView fpsOverlay;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
    private ProcessCameraProvider cameraProvider;

    private Handler fpsHandler = new Handler();
    private int frameCount = 0;
    private long startTime = System.currentTimeMillis();

    private Handler closeHandler = new Handler();
    private Runnable closeRunnable = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(CameraActivity.this, "Camera was closed to save energy.", Toast.LENGTH_LONG).show();
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        previewView = findViewById(R.id.previewView);
        imageViewFiltered = findViewById(R.id.imageViewFiltered);
        fpsOverlay = findViewById(R.id.fps_overlay);
        SeekBar numCoresSeekBar = findViewById(R.id.num_cores_seekbar);
        TextView numCoresValue = findViewById(R.id.num_cores_value);

        int numCores = Runtime.getRuntime().availableProcessors();
        numCoresSeekBar.setMax(numCores);

        numCoresSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int numCores, boolean fromUser) {
                numCoresValue.setText(String.valueOf(numCores));
                JNIHelper.setCores(numCores);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        if (!OpenCVLoader.initLocal()) {
            Toast.makeText(CameraActivity.this, "Cannot init OpenCV", Toast.LENGTH_LONG).show();
            finish();
        }

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        // Configurar el listener de un solo toque
        previewView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    // tap detected
                    toggleCamera();
                }
                return true;
            }
        });

        fpsHandler.post(fpsRunnable);

        closeHandler.postDelayed(closeRunnable, 60000);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Use case binding failed", e);
                runOnUiThread(() -> {
                    Toast.makeText(CameraActivity.this, "Failed to start camera.", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) {
            return;
        }

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {
            processFrame(image);
        });

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    private void toggleCamera() {
        Log.d(TAG, "Toggling camera");
        if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
            //Toast.makeText(this, "Front camera selected.", Toast.LENGTH_SHORT).show();
        } else {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
            //Toast.makeText(this, "Rear camera selected.", Toast.LENGTH_SHORT).show();

        }
        bindCameraUseCases();
    }

    private void processFrame(ImageProxy image) {
        frameCount++;
        if (image.getFormat() == ImageFormat.YUV_420_888) {
            ImageProxy.PlaneProxy[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            int width = image.getWidth();
            int height = image.getHeight();

            // YUV to grayscale Mat
            Mat yuvMat = new Mat(height + height / 2, width, CvType.CV_8UC1, buffer);
            Mat grayMat = new Mat();
            Imgproc.cvtColor(yuvMat, grayMat, Imgproc.COLOR_YUV2GRAY_420);

            // Do the Sobel filtering
            Mat sobelMat = JNIHelper.sobelFilterOnNewMat(grayMat);

            // Now it's time to bring the image from the Mat kingdom to Android's
            // We need to apply a transform to ensure rotations/flipping are right.
            // Mat procesado a Bitmap. TODO: reuse this matrix - make it on camera change
            Matrix matrix = new Matrix();
            if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                matrix.postRotate(270);
                matrix.postScale(-1, 1, width / 2f, height / 2f); // Horizontal flip
            } else {
                matrix.postRotate(90);
            }

            // Create empty bitmap "canvas"
            Bitmap bitmap = Bitmap.createBitmap(sobelMat.cols(), sobelMat.rows(), Bitmap.Config.ARGB_8888);

            // Draw the Mat onto the canvas
            org.opencv.android.Utils.matToBitmap(sobelMat, bitmap);

            // Transform the Canvas with our Matrix
            Bitmap finalBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            runOnUiThread(() -> {
                imageViewFiltered.setImageBitmap(finalBitmap);
            });

            image.close();
        }
    }

    /**
     * Checks whether the required camera permission was granted
     */
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    /**
     * Handles result of asking for camera permissions and starts the camera if possible.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /**
     * Calculates the FPS.
     */
    private Runnable fpsRunnable = new Runnable() {
        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - startTime;
            if (elapsedTime > 1000) {
                int fps = (int) (frameCount * 1000 / elapsedTime);
                fpsOverlay.setText(fps + " fps");
                frameCount = 0;
                startTime = currentTime;
            }
            fpsHandler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return inputFrame.rgba();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeHandler.removeCallbacks(closeRunnable);
    }
}