package com.s23d;


import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.s23d.classification.ImageClassifier;
import com.s23d.classification.Result;
import com.s23d.view.ModelActivity;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Random;


public class MainActivity extends AppCompatActivity {

    private View mainView;
    private PaintView paintView; // custom drawing view
    private ImageClassifier classifier; // complete image classification

    private TextView textViewResult;
    private TextView textViewDraw;

    //Classes declaration
    private StorageReference modelRef;
    private FirebaseStorage storage;
    //End of Classes declaration

    private boolean isConnected;
    private MaterialAlertDialogBuilder builder;
    private MaterialAlertDialogBuilder progressBuilder;
    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);

        doInit();

        // instantiate classifier
        try {
            this.classifier = new ImageClassifier(this);
        } catch (IOException e) {
            Log.e("MainActivity", "Cannot initialize tfLite model!", e);
            e.printStackTrace();
        }

        ConnectivityManager cm =
                (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        builder = new MaterialAlertDialogBuilder(this, R.style.RoundShapeTheme)
                .setTitle("No Internet")
                .setMessage("You are currently offline\nCheck you internet try again")
                .setNegativeButton("Ignore", (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton("Ok", (dialogInterface, i) -> dialogInterface.dismiss());

        progressBuilder = new MaterialAlertDialogBuilder(this, R.style.RoundShapeTheme)
                .setView(R.layout.progress);
        dialog = progressBuilder.create();

        this.mainView = this.findViewById(R.id.activity_main).getRootView();

        resetView();
    }


    private void doInit() {
        paintView = findViewById(R.id.paintView);
        paintView.init(); // initial drawing view
        textViewResult = findViewById(R.id.txt_result_label);
        textViewDraw = findViewById(R.id.txt_draw_label);
        storage = FirebaseStorage.getInstance();
    }


    public void onClearClick(View view) {
        Log.i("MainActivity", "Clear sketch event triggers");
        clearView(); //clear view and let try again
    }

    public void onDetectClick(View view) {
        Log.i("MainActivity", "Detect sketch event triggers");
        if (classifier == null) {
            Log.e("MainActivity", "Cannot initialize tfLite model!");
            return;
        }
        Bitmap sketch = paintView.getNormalizedBitmap(); // get resized bitmap
        //showImage(paintView.scaleBitmap(40, sketch));
        // create the result
        Result result = classifier.classify(sketch);
        // render results
        textViewResult.setText("");
        for (int index : result.getTopK()) {
            textViewResult.setText(
                    String.format("%s\n%s (%s%%)", textViewResult.getText(), classifier.getLabel(index),
                            String.format(Locale.getDefault(), "%.02f", classifier.getProbability(index) * 100))
            );
        }

        int expectedIndex = classifier.getExpectedIndex();
        if (result.getTopK().contains(expectedIndex)) {
            mainView.setBackgroundColor(Color.rgb(78, 175, 36));
            load3DObjectWhenClassificationIsCorrect(expectedIndex);
        } else {
            mainView.setBackgroundColor(Color.rgb(204, 0, 0));
            dialog.dismiss();
        }

    }

    private void load3DObjectWhenClassificationIsCorrect(int index) {
        if ((classifier.getProbability(index) * 100) > 50) {
            downloadAndRender3DObject(classifier.getLabel(index).toLowerCase());
        }
    }

    public void onNextClick(View view) {
        resetView();
    }

    private void downloadAndRender3DObject(String fileName) {
        if (isConnected) {
            dialog.show();
            modelRef = storage.getReference().child(fileName + ".obj");

            final File localFile = new File(getCacheDir(), fileName + ".obj");
            modelRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                Log.i("Renderer ", ";local tem file created  created " + localFile.toString());
                Log.i("Renderer ", ";local tem file dir" + localFile.getAbsolutePath());
                Intent intent = new Intent(MainActivity.this.getApplicationContext(), ModelActivity.class);
                // ContentUtils.setCurrentDir(localFile.getParentFile());
                Log.i("Renderer ", ";local tem file dir: file://" + localFile.getAbsolutePath());
                intent.putExtra("uri", "file://" + localFile.getPath());
                intent.putExtra("immersiveMode", "true");
                MainActivity.this.startActivity(intent);
            }).addOnFailureListener(exception -> {
                Log.i("Renderer ", ";local tem file not created  created " + exception.toString());
                dialog.dismiss();
            });
        } else
            builder.show();
    }

    // debug: ImageView with rescaled 28x28 bitmap
    private void showImage(Bitmap bitmap) {
        Dialog builder = new Dialog(this);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.getWindow().setBackgroundDrawable(
                new ColorDrawable(android.graphics.Color.TRANSPARENT));
        builder.setOnDismissListener(dialogInterface -> {
        });

        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(bitmap);
        builder.addContentView(imageView, new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        builder.show();
    }

    private void resetView() {
        mainView.setBackgroundColor(Color.WHITE);
        paintView.clear();
        textViewResult.setText("");
        // get a random label and set as expected class
        classifier.setExpectedIndex(new Random().nextInt(classifier.getNumberOfClasses()));
        textViewDraw.setText(String.format("Draw ... %s", classifier.getLabel(classifier.getExpectedIndex())));
    }


    private void clearView() {
        paintView.clear();
        textViewResult.setText("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}