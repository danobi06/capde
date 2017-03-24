package com.example.dobi.seek;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity {

    ImageView mImageView;
    String mCurrentPhotoPath;
    private Bitmap mImageBitmap;

    private static String TAG = "MainActivity";
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;

    /* Local Variables */
    int radius = 100;
    double thresh = 190; //optimal 150
    int bw_width = 170; //#170
    Mat mMat;

    Bitmap result;
    //Mat mBlur = new Mat(); //blur variable
    //Mat mThresh1 = new Mat(); //threshold 1
    //Mat mEdges = new Mat();
    //Mat mSobelx = new Mat();
    ////Mat mSobely = new  Mat();
    Mat mHierarchy;
    Mat mDrawing;
    java.util.List<MatOfPoint>  mContours;
    int numRectangles = 0;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onResume(){
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImageView = (ImageView) findViewById(R.id.imageView);
        mImageBitmap = null;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        Log.d(TAG, "Creating file name!");
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        Log.d(TAG, "mCurrentPhotoPath "+ mCurrentPhotoPath);
        Log.d(TAG, "Finished creating file name!!");
        return image;
    }
    private void setPic() {

		/* There isn't enough memory to open up more than a couple camera photos */
		/* So pre-scale the target bitmap into which the file is decoded */

		/* Get the size of the ImageView */
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

		/* Get the size of the image */
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

		/* Figure out which way needs to be reduced less */
        int scaleFactor = 1;
        if ((targetW > 0) || (targetH > 0)) {
            scaleFactor = Math.min(photoW/targetW, photoH/targetH);
        }

		/* Set bitmap options to scale the image decode target */
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;


		/* Decode the JPEG file into a Bitmap */
        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        //showGray(bitmap, mMat);
        if (process(bitmap)) {
            mImageView.setImageBitmap(bitmap);
        }
        else{
            Canvas canvas = new Canvas();

            canvas.drawText("No Crosswalks Dectected!",
                    mImageView.getWidth()/2,
                    mImageView.getHeight()/2, new Paint(Color.RED));
            mImageView.draw(canvas);
        }
        /* Associate the Bitmap to the ImageView */

        mImageView.setVisibility(View.VISIBLE);
    }

    public void openCameraApp(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.d(TAG, "Error occurred while creating the file!");

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Log.d(TAG, "PhotoFile Not Null!");

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                Log.d(TAG, "Returned");
            }
        }
    }

    private void handleCameraPhoto() {

        if (mCurrentPhotoPath != null) {
            setPic();
            Log.d(TAG, "setPic()");
            galleryAddPic();
            mCurrentPhotoPath = null;
        }

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Log.d(TAG, "Got request code");
            handleCameraPhoto();
        }
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }
    public Bitmap showGray(Bitmap bMap, Mat mMat){
        Bitmap result = Bitmap.createBitmap(bMap);
        mMat = new Mat();

        Utils.bitmapToMat(bMap, mMat);

        Mat mGray = new Mat(mMat.height(), mMat.height(), CvType.CV_8UC1);
        Imgproc.cvtColor(mMat, mGray, Imgproc.COLOR_BGR2GRAY);

        Utils.matToBitmap(mGray, result);
        Log.d(TAG, "showing gray");

        return result;
    }
    public boolean process(Bitmap bMap){
        mMat = new Mat(); //holds bitMap conversion

        result = Bitmap.createBitmap(bMap);
        /* convert Original to Bitmap to Mat */
        Utils.bitmapToMat(bMap, mMat);
        Log.d(TAG, "convert Original to Bitmap to Mat");

        /* Gray scale */
        Mat mGray = new Mat(mMat.size(), CvType.CV_8UC1);
        Imgproc.cvtColor(mMat, mGray, Imgproc.COLOR_RGB2GRAY);
        Log.d(TAG, "Gray Scale");

        /* GuassianBlur */
        //Size size = new Size(5,5);
        //Imgproc.GaussianBlur(mGray, mBlur, size,0);

        /*Threshold 1 */
        Mat mThresh1 = new Mat(mGray.size(), mGray.type());
        Imgproc.threshold(mGray, mThresh1,thresh, 255, 0);
        Log.d(TAG, "Threshold1");

       /* Canny */
        Mat mEdges = new Mat(mThresh1.size(), mThresh1.type());

        Imgproc.Canny(mThresh1, mEdges, thresh, thresh*2);
        Log.d(TAG, "Canny");

        /* Sobelx */
        //Imgproc.Sobel(mThresh1, mSobelx, CvType.CV_8UC1, 1,0);
        //Log.d(TAG, "sobelx");

        /* Sobely */
        //Imgproc.Sobel(mThresh1, mSobely, CvType.CV_8UC1, 0,1);

        /* findContours */
        mHierarchy = new Mat();
        mDrawing = new  Mat();
        mContours = new ArrayList<>();
        Imgproc.findContours(mEdges, mContours, mHierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        Log.d(TAG, "findContours");

        /*** draw lines ***/
        java.util.List<Integer>  bxLeft = new ArrayList<>();
        java.util.List<Integer>  byLeft = new ArrayList<>();
        java.util.List<Point> bxbyLeftArray = new ArrayList<>();
        java.util.List<Point>  bxbyRightArray = new ArrayList<>();
        java.util.List<Integer> bxRight = new ArrayList<>();
        java.util.List<Integer>  byRight = new ArrayList<>();
        Log.d(TAG, "DrawLines 1");

        Log.d(TAG, "Contour size is  " + mContours.size());
        for (MatOfPoint i : mContours) {
            Rect rect = Imgproc.boundingRect(i);
            Scalar lineColor = new Scalar(0, 255,0);
            Imgproc.drawContours(mDrawing, mContours,0, lineColor);
            //Log.d(TAG, "DrawLines 2");

            if(rect.width > bw_width){
                Imgproc.line(mMat, new Point(rect.x, rect.y), new Point((rect.x + rect.width), (rect.y + rect.height)), lineColor);
                bxRight.add(rect.x + rect.width);
                byRight.add(rect.y + rect.height);
                bxLeft.add(rect.x);
                byLeft.add(rect.y);
                //int[] lArray = {rect.x, rect.y};
                bxbyLeftArray.add(new Point(rect.x, rect.y));
                //int[] rArray = {(rect.x + rect.width), (rect.y + rect.height)};
                bxbyRightArray.add(new Point((rect.x + rect.width), (rect.y + rect.height)));
                Scalar pinkCircle = new Scalar(0, 255,255);

                Imgproc.circle(mMat, new Point(rect.x,rect.y), 10, pinkCircle, 2);
                Imgproc.circle(mMat, new Point((rect.x +rect.width),(rect.y + rect.height)), 10, pinkCircle, 2);
                Log.d(TAG, "Found a rectangle");

                numRectangles++;
            }

        }
        if(numRectangles < 5){
            Log.d(TAG, "Failed -> False");
            return false;
        }
        Log.d(TAG, "contour done!");

        /* Get medianL */
        Collections.sort(bxLeft);
        Collections.sort(byLeft);
        Log.d(TAG, "bxLeft size " + bxLeft.size()/2);
        Log.d(TAG, "byLeft size " + byLeft.size()/2);

        Point MedianL = new Point(bxLeft.get((bxLeft.size()/2)), byLeft.get((byLeft.size()/2)));
        Log.d(TAG, "Get MedianL");

        /* Get MedianR */
        Collections.sort(bxRight);
        Collections.sort(byRight);
        Point MedianR = new Point(bxRight.get((bxRight.size()/2)), byRight.get((byRight.size()/2)));
        Log.d(TAG, "Get MedianR");

        /* Draw Black Circle */
        Scalar blackCircle = new Scalar(0, 0,0);
        Imgproc.circle(mMat, new Point(MedianL.x, MedianL.y), radius, blackCircle,2);
        Imgproc.circle(mMat, new Point(MedianR.x, MedianR.y), radius, blackCircle,2);
        Log.d(TAG, "Draw Black Circle");


//        /* Get AverageL */
//        int xSum = 0;
//        for(Integer i: bxLeft ){
//            xSum+=i;
//        }
//
//        int ySum = 0;
//        for(Integer i: byLeft ){
//            ySum+=i;
//        }
//        Point AverageL = new Point(xSum/bxLeft.size(), ySum/byLeft.size());
//
//         /* Get AverageR */
//        xSum = 0;
//        for(Integer i: bxRight ){
//            xSum+=i;
//        }
//
//        ySum = 0;
//        for(Integer i: byRight ){
//            ySum+=i;
//        }
//        Point AverageR = new Point(xSum/bxRight.size(), ySum/byRight.size());
//

        /*** Bounded within a Circle ***/
        int cnt = 0;
        java.util.List<Point>  boundedLeft = new ArrayList<>();
        java.util.List<Point>  boundedRight = new ArrayList<>();
        Log.d(TAG, "Bounded Circle 1");

        for(Point i: bxbyLeftArray){
            if((Math.pow((MedianL.x - i.x), 2) + Math.pow((MedianL.y - i.y), 2) < Math.pow(radius, 2))){
                boundedLeft.add(i);
                cnt++;
            }
        }
        Log.d(TAG, "Bounded Circle 2");
        cnt = 0;
        for(Point i: bxbyRightArray){
            if((Math.pow((MedianR.x - i.x), 2) + Math.pow((MedianR.y - i.y), 2) < Math.pow(radius, 2))){
                boundedRight.add(i);
                cnt++;
            }
        }
        Log.d(TAG, "Bounded Circle 3");
        /* Left Bound */
        MatOfPoint boundedL = new MatOfPoint();
        boundedL.fromList(boundedLeft);
        Mat lLine = new Mat();
        Imgproc.fitLine(boundedL, lLine, Imgproc.DIST_L2, 0, 0.01,0.01 );
        Log.d(TAG, "BoundL");

        /* Right Bound */
        MatOfPoint boundedR = new MatOfPoint();
        boundedR.fromList(boundedRight);
        Mat rLine = new Mat();
        Imgproc.fitLine(boundedR, rLine, Imgproc.DIST_L2, 0, 0.01,0.01 );
        Log.d(TAG, "BoundR");

        int m = radius*10;
        Scalar redLine = new Scalar(255, 0,0);

        /* Draw Lines */
        Imgproc.line(mMat, new Point((lLine.cols() - lLine.width()*m), (lLine.rows()-m*lLine.height())),
                new Point((rLine.cols() + rLine.width()*m), (lLine.rows()+m*lLine.height())),
                redLine, 3);
        Log.d(TAG, "Draw Lines 1");

        Imgproc.line(mMat, new Point((rLine.cols() - rLine.width()*m), (rLine.rows()-m*rLine.height())),
                new Point((rLine.cols() + rLine.width()*m), (rLine.rows()+m*rLine.height())),
                redLine, 3);
        Log.d(TAG, "Draw Lines 2");

        /* Convert back to bitMap */
        Utils.matToBitmap(mMat, result);
        Log.d(TAG, "processed -> True");

        return true;
    }

}