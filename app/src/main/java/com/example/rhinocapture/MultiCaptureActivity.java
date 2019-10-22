package com.example.rhinocapture;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.media.MediaActionSound;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MultiCaptureActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {

    private static final String TAG = MultiCapture.class.getSimpleName();
    private MultiCapture cameraBridgeViewBase;
    private Mat colorRgba;
    private View decorView;
    private Button captureBtn;
    private Button extractBtn;
    private static List<Mat> images = new ArrayList<Mat>();
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_multi_capture);
        decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        cameraBridgeViewBase = findViewById(R.id.camera_view);
        cameraBridgeViewBase.setVisibility(View.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);
        cameraBridgeViewBase.setFocusable(true);
        cameraBridgeViewBase.setFocusMode(this, 0);
        cameraBridgeViewBase.setMode();
        cameraBridgeViewBase.setOnTouchListener(this);
        captureBtn = findViewById(R.id.captureBtn);
        extractBtn = findViewById(R.id.processingBtn);
        progressDialog = new ProgressDialog(this);

        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaActionSound sound = new MediaActionSound();
                sound.play(MediaActionSound.SHUTTER_CLICK);
                cameraBridgeViewBase.takePicture();
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
        });

        extractBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(images.size() != 0){
                    Mat[] mats = images.toArray(new Mat[images.size()]);
                    MultiWatershed mw = new MultiWatershed(getApplicationContext());
                    mw.execute(mats);
                }

                else {
                    AlertDialog.Builder ab = new AlertDialog.Builder(MultiCaptureActivity.this);
                    ab.setTitle("INFO");
                    ab.setMessage("No photo captured. Please take a photo.");
                    ab.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
                        }
                    });

                    ab.create().show();
                }



            }
        });


    }

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    cameraBridgeViewBase.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    public void onCameraViewStarted(int width, int height) {
        colorRgba = new Mat(height, width, CvType.CV_8UC4);

    }

    @Override
    public void onCameraViewStopped() {
        colorRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        colorRgba = inputFrame.rgba();
        return colorRgba;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, baseLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        cameraBridgeViewBase.focusOnTouch(motionEvent);
        return true;
    }

    public static class MultiCapture extends JavaCameraView implements Camera.PictureCallback, Camera.AutoFocusCallback {

        private static Context context;
        private final String TAG = MultiCaptureActivity.class.getSimpleName();

        public MultiCapture(Context context, AttributeSet attrs) {
            super(context, attrs);
            this.context = context;
            mCamera = Camera.open();
        }

        public void takePicture() {

            Log.i(TAG, "Taking Picture");
            mCamera.setPreviewCallback(null);
            mCamera.takePicture(null, null, this);

        }

        @Override
        public void onPictureTaken(byte[] bytes, Camera camera) {

            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            Mat orig = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC3);
            Bitmap bitmap32 = bmp.copy(Bitmap.Config.ARGB_8888, true);
            Utils.bitmapToMat(bitmap32, orig);
            Imgproc.cvtColor(orig, orig, Imgproc.COLOR_BGR2RGB);
            images.add(orig);
            mCamera.startPreview();
            mCamera.setPreviewCallback(this);
        }

        @Override
        public void onAutoFocus(boolean b, Camera camera) {

        }

        public Camera.Size getResolution() {
            Camera.Parameters params = mCamera.getParameters();
            Camera.Size s = params.getPreviewSize();
            return s;
        }

        public void setResolution(Camera.Size resolution) {
            disconnectCamera();
            connectCamera((int) resolution.width, (int) resolution.height);
        }

        public void focusOnTouch(MotionEvent event) {
            android.graphics.Rect focusRect = calculateTapArea(event.getRawX(), event.getRawY(), 1f);
            android.graphics.Rect meteringRect = calculateTapArea(event.getRawX(), event.getRawY(), 1.5f);

            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

            if (parameters.getMaxNumFocusAreas() > 0) {
                List<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
                focusAreas.add(new Camera.Area(focusRect, 1000));

                parameters.setFocusAreas(focusAreas);
            }

            if (parameters.getMaxNumMeteringAreas() > 0) {
                List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
                meteringAreas.add(new Camera.Area(meteringRect, 1000));

                parameters.setMeteringAreas(meteringAreas);
            }

            mCamera.setParameters(parameters);
            mCamera.autoFocus(this);
        }

        /**
         * Convert touch position x:y to {@link Camera.Area} position -1000:-1000 to 1000:1000.
         */
        private Rect calculateTapArea(float x, float y, float coefficient) {
            float focusAreaSize = 300;
            int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();

            int centerX = (int) (x / getResolution().width - 1000);
            int centerY = (int) (y / getResolution().height - 1000);

            int left = clamp(centerX - areaSize / 2, -1000, 1000);
            int top = clamp(centerY - areaSize / 2, -1000, 1000);

            RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);

            return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
        }

        private int clamp(int x, int min, int max) {
            if (x > max) {
                return max;
            }
            if (x < min) {
                return min;
            }
            return x;
        }

        public void setFocusMode(Context item, int type) {

            Camera.Parameters params = mCamera.getParameters();
            List<String> FocusModes = params.getSupportedFocusModes();

            switch (type) {
                case 0:
                    if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    else
                        Toast.makeText(item, "Auto Mode not supported", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    else
                        Toast.makeText(item, "Continuous Mode not supported", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_EDOF))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_EDOF);
                    else
                        Toast.makeText(item, "EDOF Mode not supported", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_FIXED))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                    else
                        Toast.makeText(item, "Fixed Mode not supported", Toast.LENGTH_SHORT).show();
                    break;
                case 4:
                    if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_INFINITY))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
                    else
                        Toast.makeText(item, "Infinity Mode not supported", Toast.LENGTH_SHORT).show();
                    break;
                case 5:
                    if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_MACRO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
                    else
                        Toast.makeText(item, "Macro Mode not supported", Toast.LENGTH_SHORT).show();
                    break;
            }

            mCamera.setParameters(params);

        }

        public void setMode() {

            Camera.Parameters params = mCamera.getParameters();

            params.setAutoExposureLock(false);
            params.setAutoWhiteBalanceLock(false);
            params.setExposureCompensation(params.getMaxExposureCompensation());

            if (params.getSupportedWhiteBalance().contains(Camera.Parameters.WHITE_BALANCE_DAYLIGHT)){

                params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_DAYLIGHT);
            }

            if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {

                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }

            mCamera.setParameters(params);

        }


    }

    public class MultiWatershed extends AsyncTask<Mat[], Integer, Void> {

        Context mContext;


        public MultiWatershed(Context context) {
            this.mContext = context;
        }

        @Override
        protected Void doInBackground(Mat[]... mats) {
            Mat[] images = mats[0];

            for (int i=0; i<images.length; i++){
                extract(images[i], detect(images[i]), i);
            }

            return null;
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setCancelable(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage("Extraction");
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();
            mContext.startActivity(new Intent(mContext,GalleryActivity.class));
        }

        public Mat detect(Mat src) {

            Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2RGB);
            Mat brightness = new Mat(src.rows(), src.cols(), src.type());
            //Mat dest = new Mat();
            //Mat res = new Mat();
            Mat mat = new Mat();

            //Imgproc.GaussianBlur(src, brightness, new Size(0,0), 10);
            src.convertTo(brightness, -1, 2.5, 10);
            //Core.addWeighted(brightness,1.5, brightness, 0.2, 1.4, brightness);

            //saveImage(brightness, "lumin", false);
            Mat fnl = new Mat();
            //Photo.fastNlMeansDenoisingColored(brightness,fnl);
            Mat blur = new Mat();
            // Imgproc.medianBlur(fnl,blur,15);
            // Imgproc.bilateralFilter(blur,mat,15,80,80, Core.BORDER_DEFAULT);
            Mat shifted = new Mat();
            fnl = this.performGammaCorrection(brightness, 2);
            Imgproc.pyrMeanShiftFiltering(fnl, shifted, 21, 51);
            //saveImage(shifted, "pyr", false);


            //Scala di grigi
            Mat gray = new Mat();
            //Mat hist = new Mat();
            Imgproc.cvtColor(shifted, gray, Imgproc.COLOR_RGB2GRAY);
            //saveImage(gray, "grigio", false);
            // Imgproc.calcHist(Arrays.asList(gray),new MatOfInt(0), new Mat(), hist, new MatOfInt(256), new MatOfFloat(0,256));
            // int histSize = hist.height();

            //Otsu
            Mat binary = new Mat();
            Imgproc.threshold(gray, binary, 0, 255, Imgproc.THRESH_BINARY_INV | Imgproc.THRESH_OTSU);
            //saveImage(binary, "binario", false);

            //Dilatazione
            Mat kernel = Mat.ones(3, 3, CvType.CV_8U);
            Mat background = new Mat();
            Mat ker = Mat.ones(5, 5, CvType.CV_8U);
            Imgproc.dilate(binary, background, kernel);
            Imgproc.morphologyEx(background, background, Imgproc.MORPH_OPEN, ker);
            //saveImage(background, "sfondo", false);


            //Distance Transform
            Mat foreground = new Mat(src.size(), CvType.CV_8U);
            Imgproc.distanceTransform(binary, foreground, Imgproc.DIST_L2, 5);
            Core.normalize(foreground, foreground, 0.0, 1.0, Core.NORM_MINMAX);
            foreground.convertTo(foreground, CvType.CV_8UC1, 255, 0);
            Imgproc.threshold(foreground, foreground, 150, 255, Imgproc.THRESH_BINARY);
            //saveImage(foreground, "contenuto", false);

            //Subtract
            Mat unknown = new Mat();
            Core.subtract(background, foreground, unknown);
            //saveImage(unknown, "sconosciuto", false);

            //Connected Components
            Mat labels = new Mat();
            Imgproc.connectedComponents(foreground, labels);

            for (int x = 0; x < labels.width(); x++) {

                for (int y = 0; y < labels.height(); y++) {
                    double[] borderpixel = unknown.get(y, x);

                    if (borderpixel[0] == 255) {
                        labels.put(y, x, 0);
                    }
                }
            }

            //Watershed
            Mat mask = new Mat();
            Imgproc.dilate(binary, mask, new Mat());
            Mat sourcecrop = new Mat(shifted.size(), shifted.type());
            shifted.copyTo(sourcecrop, mask);
            Imgproc.watershed(sourcecrop, labels);
            //saveImage(sourcecrop, "watershed", false);

            Imgproc.cvtColor(sourcecrop, sourcecrop, Imgproc.COLOR_RGB2GRAY);

            for (int i = 1; i < labels.rows() - 1; i++) {
                for (int j = 1; j < labels.cols() - 1; j++) {
                    if (labels.get(i, j)[0] == -1) {
                        sourcecrop.put(i, j, 0);
                    }
                }
            }

            //Imgproc.pyrMeanShiftFiltering(sourcecrop,sourcecrop,21,51;
            // saveImage(sourcecrop,"waterpyr");

            Imgproc.erode(sourcecrop, sourcecrop, Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(8,8)));

            //Canny
            Mat cannyedges = new Mat();
            Mat graywatershed = new Mat();
            Mat kk = Mat.ones(3, 3, CvType.CV_8U);
            //Imgproc.cvtColor(sourcecrop, graywatershed, Imgproc.COLOR_RGB2GRAY);
            Imgproc.Canny(sourcecrop, cannyedges, 100, 100 * 3);
            Imgproc.morphologyEx(cannyedges, cannyedges, Imgproc.MORPH_CLOSE, kk);
            //saveImage(cannyedges, "canny", false);

            brightness.release();
            background.release();
            binary.release();
            foreground.release();
            graywatershed.release();
            sourcecrop.release();
            labels.release();
            shifted.release();
            unknown.release();
            kernel.release();
            gray.release();
            mat.release();
            mask.release();
            fnl.release();
            blur.release();
            return cannyedges;
        }

        private Mat performGammaCorrection(Mat matImgSrc, double gammaValue) {
            //! [changing-contrast-brightness-gamma-correction]
            Mat lookUpTable = new Mat(1, 256, CvType.CV_8U);
            byte[] lookUpTableData = new byte[(int) (lookUpTable.total() * lookUpTable.channels())];
            for (int i = 0; i < lookUpTable.cols(); i++) {
                lookUpTableData[i] = saturate(Math.pow(i / 255.0, gammaValue) * 255.0);
            }
            lookUpTable.put(0, 0, lookUpTableData);
            Mat img = new Mat();
            Core.LUT(matImgSrc, lookUpTable, img);
            return img;
        }

        private byte saturate(double val) {
            int iVal = (int) Math.round(val);
            iVal = iVal > 255 ? 255 : (iVal < 0 ? 0 : iVal);
            return (byte) iVal;
        }

        private void saveImage(Mat image, String name, boolean formatPng){
            OutputStream outputStream;
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + "Segmentation/" + "Session/");
            if (!dir.exists()){
                dir.mkdirs();
            }

            if (formatPng == true){
                File complete = new File(dir,name + ".png");
                Bitmap bp = Bitmap.createBitmap(image.cols(),image.rows(),Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(image,bp);
                try{
                    outputStream = new FileOutputStream(complete);
                    bp.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    outputStream.flush();
                    outputStream.close();
                } catch (FileNotFoundException e){
                    e.printStackTrace();
                } catch (IOException ex){
                    ex.printStackTrace();
                }
            }

            else{
                File complete = new File(dir,name + ".jpg");
                Bitmap bp = Bitmap.createBitmap(image.cols(),image.rows(),Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(image,bp);

                try{
                    outputStream = new FileOutputStream(complete);
                    bp.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    outputStream.flush();
                    outputStream.close();
                } catch (FileNotFoundException e){
                    e.printStackTrace();
                } catch (IOException ex){
                    ex.printStackTrace();
                }
            }


        }


        public void extract(Mat src, Mat canny, int j){
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(canny, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            MatOfPoint2f[] contoursPoly = new MatOfPoint2f[contours.size()];
            org.opencv.core.Rect[] boundRect = new org.opencv.core.Rect[contours.size()];
            for (int i = 0; i < contours.size(); i++) {
                contoursPoly[i] = new MatOfPoint2f();
                Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), contoursPoly[i], 3, true);
                boundRect[i] = Imgproc.boundingRect(new MatOfPoint(contoursPoly[i].toArray()));
            }

            List<MatOfPoint> contoursPolyList = new ArrayList<>(contoursPoly.length);
            for (MatOfPoint2f poly : contoursPoly) {
                contoursPolyList.add(new MatOfPoint(poly.toArray()));
            }

            List<Mat> cropped = new ArrayList<>(boundRect.length);

            for (int i=0; i<contours.size(); i++) {

                if ((boundRect[i].area() > 50000 && boundRect[i].area() < 1200000) || (boundRect[i].area() < 1200000 && boundRect[i].area() > 50000)) {
                    //Imgproc.drawContours(src, contoursPolyList, i, new Scalar(0,0,0));
                    boundRect[i] = enlargeRoi(src, boundRect[i], 100);
                    //Imgproc.rectangle(src, boundRect[i], new Scalar(0, 0, 255));
                    cropped.add(src.submat(boundRect[i]));

                }


            }

            //saveImage(src, "Result " + System.currentTimeMillis(), true);

            for(int i=0 ; i<cropped.size(); i++){
                saveImage(cropped.get(i), "Extraction " + j + " number " + i, true);
            }

            hierarchy.release();
            canny.release();
        }

        private org.opencv.core.Rect enlargeRoi (Mat src, org.opencv.core.Rect boundRect, int padding) {

            org.opencv.core.Rect returnRect = new org.opencv.core.Rect(boundRect.x - padding, boundRect.y - padding, boundRect.width + (padding * 2), boundRect.height + (padding * 2));
            if (returnRect.x < 0) returnRect.x = 0;
            if (returnRect.y < 0) returnRect.y = 0;
            if (returnRect.x + returnRect.width >= src.cols())
                returnRect.width = src.cols() - returnRect.x;
            if (returnRect.y + returnRect.height >= src.rows())
                returnRect.height = src.rows() - returnRect.y;
            return returnRect;
        }

    }
}
