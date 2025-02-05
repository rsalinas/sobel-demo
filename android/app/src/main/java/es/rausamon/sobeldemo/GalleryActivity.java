/**
 * Activity for applying the filter to an image residing in our gallery.
 */

package es.rausamon.sobeldemo;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class GalleryActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private ImageView imageView;
    private ProgressBar progressBar;
    private Bitmap originalBitmap;
    private Bitmap filteredBitmap;
    private boolean showingOriginal = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        imageView = findViewById(R.id.imageView);
        progressBar = findViewById(R.id.progressBar);

        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);

        imageView.setOnClickListener(v -> toggleImage());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Let's see if an image was picked in the image selector.
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                // Keep a copy of the original bitmap in the activity so that we can toggle later
                originalBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);

                processImage(originalBitmap);
            } catch (Exception e) {
                Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void processImage(Bitmap bitmap) {
        // First we display the original image while we process it,
        // And we enable the "busy" indicator, so that the user knows something is going on.
        runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
            imageView.setImageBitmap(bitmap);
        });

        new Thread(() -> {
            // Bring camera's Bitmap into OpenCV's world
            Mat inputMat = JNIHelper.bitmapToMat(bitmap);

            // Convert to grayscale
            Mat grayMat = new Mat();
            Imgproc.cvtColor(inputMat, grayMat, Imgproc.COLOR_RGB2GRAY);

            // Apply our filter
            Mat outputMat = JNIHelper.sobelFilterOnNewMat(grayMat);

            // Export it back to something we can visualize on screen
            filteredBitmap = JNIHelper.matToBitmap(outputMat);

            // Disable the progress indication and show the image.
            runOnUiThread(() -> {
                imageView.setImageBitmap(filteredBitmap);
                progressBar.setVisibility(View.GONE);

                // Inform the user they can alternate between the original and the filtered images.
                Toast.makeText(this, "Touch the image to toggle.", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    /**
     * Toggles the shown imagge between the original one and the filtered one.
     */
    private void toggleImage() {
        showingOriginal = !showingOriginal;
        if (showingOriginal) {
            imageView.setImageBitmap(originalBitmap);
        } else {
            imageView.setImageBitmap(filteredBitmap);
        }
    }
}