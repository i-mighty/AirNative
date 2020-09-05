package jtheiner.drawingclassification;


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.andresoviedo.util.android.ContentUtils;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import jtheiner.drawingclassification.classification.ImageClassifier;
import jtheiner.drawingclassification.classification.Result;
import jtheiner.drawingclassification.view.ModelActivity;


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);


        paintView = findViewById(R.id.paintView);
        paintView.init(); // initial drawing view
        textViewResult = findViewById(R.id.txt_result_label);
        textViewDraw = findViewById(R.id.txt_draw_label);

        storage = FirebaseStorage.getInstance();

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
                .setMessage("You are currently offline\nCheck you internet to continue")
                .setNegativeButton("Ignore", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

        this.mainView = this.findViewById(R.id.activity_main).getRootView();

        resetView();
    }


    public void onClearClick(View view) {
        Log.i("MainActivity", "Clear sketch event triggers");
        paintView.clear();
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
                    String.format("%s\n%s (%s%%)", textViewResult.getText(), classifier.getLabel(index), String.format("%.02f", classifier.getProbability(index) * 100))
            );
            if ((classifier.getProbability(index) * 100) > 50) {
                Toast.makeText(this, "Loading 3D Model for " + classifier.getLabel(index), Toast.LENGTH_SHORT).show();
                downloadFile(classifier.getLabel(index).toLowerCase());
            }
        }

        int expectedIndex = classifier.getExpectedIndex();
        if (result.getTopK().contains(expectedIndex)) {
            mainView.setBackgroundColor(Color.rgb(78, 175, 36));
        } else {
            mainView.setBackgroundColor(Color.rgb(204, 0, 0));
        }

    }

    public void onNextClick(View view) {
        resetView();
    }

    private void downloadFile(String fileName) {
        if (isConnected) {
            modelRef = storage.getReference().child(fileName + ".obj");

            final File localFile = new File(getCacheDir(), fileName + ".obj");
            modelRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Log.i("Renderer ", ";local tem file created  created " + localFile.toString());
                    Log.i("Renderer ", ";local tem file dir" + localFile.getAbsolutePath());
                    Intent intent = new Intent(MainActivity.this.getApplicationContext(), ModelActivity.class);
                    ContentUtils.setCurrentDir(localFile.getParentFile());
                    Log.i("Renderer ", ";local tem file dir: file://" + localFile.getAbsolutePath());
                    intent.putExtra("uri", "file://" + localFile.getPath());
                    intent.putExtra("immersiveMode", "true");
                    MainActivity.this.startActivity(intent);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.i("Renderer ", ";local tem file not created  created " + exception.toString());
                }
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
        textViewDraw.setText("Draw ... " + classifier.getLabel(classifier.getExpectedIndex()));
    }
}