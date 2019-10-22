package com.example.rhinocapture;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private Button fastButton;
    private Button multiButton;
    private Button infoButton;
    private Button galleryButton;
    private Button quitBtn;
    private static final int MY_PERMISSION_REQUEST_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Log.e("Info", this.getFilesDir().toString());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

            checkPermissions();
        }

        else {
            fastCapture();
            multiCapture();
            infoHowto();
            quit();
        }
    }

    public void fastCapture(){
        fastButton =  findViewById(R.id.fastBtn);
        fastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Utils.isEmptyGallery()){
                    openActivityFast();
                }

                else {
                    android.app.AlertDialog.Builder ab = new android.app.AlertDialog.Builder(MainActivity.this);
                    ab.setTitle("ATTENTION");
                    ab.setMessage("If you press the YES button previously extracted cells will be deleted from gallery. " +
                            " Are you sure to proceed with a new extraction?");

                    ab.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            Utils.clearGallery();
                            openActivityFast();
                        }
                    });

                    ab.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            openGallery();
                        }
                    });

                    ab.create().show();
                }

            }
        });
    }

    public void multiCapture(){
        multiButton = findViewById(R.id.multipleBtn);
        multiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                if (Utils.isEmptyGallery()){
                    openActivityMulti();
                }

                else {
                    android.app.AlertDialog.Builder ab = new android.app.AlertDialog.Builder(MainActivity.this);
                    ab.setTitle("ATTENTION");
                    ab.setMessage("If you press the YES button previously extracted cells will be deleted from gallery. " +
                            " Are you sure to proceed with a new extraction?");

                    ab.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            Utils.clearGallery();
                            openActivityMulti();
                        }
                    });

                    ab.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            openGallery();
                        }
                    });

                    ab.create().show();
                }
            }
        });
    }


    public void galleryOpen(){
        galleryButton = findViewById(R.id.galleryBtn);
        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (Utils.isEmptyGallery()){
                    AlertDialog.Builder ab = new AlertDialog.Builder(MainActivity.this);
                    ab.setTitle("INFO");
                    ab.setMessage("Gallery is empty.");
                    ab.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });

                    ab.create().show();

                }

                else {

                    openGallery();
                }



            }
        });
    }


    public void infoHowto(){
        infoButton = findViewById(R.id.infoBtn);
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivityInfo();

            }
        });
    }

    public void quit(){
        quitBtn = findViewById(R.id.quitBtn);
        quitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder ab = new AlertDialog.Builder(MainActivity.this);
                ab.setTitle("EXIT");
                ab.setMessage("Are you sure you want to quit ?");
                ab.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        closeApp();
                    }
                });

                ab.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });

                ab.create().show();
            }
        });
    }

    public void openActivityFast(){
        Intent intent = new Intent(this,FastCaptureActivity.class);
        startActivity(intent);
    }

    public void openActivityMulti(){
        Intent intent = new Intent(this, MultiCaptureActivity.class);
        startActivity(intent);
    }

    public void openActivityInfo() {
        Intent intent = new Intent(this, InfoActivity.class);
        startActivity(intent);
    }

    public void openGallery(){
        Intent intent = new Intent(this, GalleryActivity.class);
        startActivity(intent);
    }

    public void closeApp(){
        this.finishAffinity();
        System.exit(0);
    }

    private void checkPermissions(){

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) + ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) +
        ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED){

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.INTERNET)) {

                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setMessage("Camera, Storage and Internet permissions are required");
                dialog.setTitle("Please grant those permissions");
                dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                                Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET
                        }, MY_PERMISSION_REQUEST_CODE);
                    }
                });

                dialog.setNeutralButton("Cancel", null);
                dialog.create().show();

            }

            else {

                ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                        Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET
                }, MY_PERMISSION_REQUEST_CODE);
            }
        }

        else{
            fastCapture();
            multiCapture();
            galleryOpen();
            infoHowto();
            quit();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){

            case MY_PERMISSION_REQUEST_CODE:{

                if (grantResults.length > 0 && grantResults[0] + grantResults[1] + grantResults[2] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
                    fastCapture();
                    multiCapture();
                    galleryOpen();
                    infoHowto();
                    quit();
                }

                else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                    checkPermissions();
                }
            }

        }
    }
}
