package com.example.dobi.seek;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Created by dobi on 4/12/17.
 */

public class CrossWalkDectector {

    ImageView mImageView;
    String mCurrentPhotoPath;
    private Bitmap mImageBitmap;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;

    /* Local Variables */
    int radius = 100;
    double thresh = 190; //optimal 150
    int bw_width = 170; //#170

    //Cache
    //Mat rgbaImage = new Mat(); //image to process
    Mat mGray = new Mat(); //grey Scale
    Mat mThresh1 = new Mat();
    Mat mEdges = new Mat();
    Mat mHierarchy = new Mat();
    Mat mDrawing = new Mat();
    private List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();
    int numRectangles = 0;

    java.util.List<Integer> bxLeft = new ArrayList<>();
    java.util.List<Integer> byLeft = new ArrayList<>();
    java.util.List<Point> bxbyLeftArray = new ArrayList<>();
    java.util.List<Point> bxbyRightArray = new ArrayList<>();
    java.util.List<Integer> bxRight = new ArrayList<>();
    java.util.List<Integer> byRight = new ArrayList<>();

    //lineCalc
    public double[] lineCalc(double vx, double vy, double x0, double y0 ){
        double scale =10;
        double x1 = x0+scale*vx;
        double y1 = y0+scale*vy;
        double m = (y1-y0)/(x1-x0);
        double b = y1-m*x1;

        double val[] = {m,b};
        return val;

    }

    //angle
    public double angle(Point pt1, Point pt2){
        double x1 = pt1.x;
        double y1 = pt1.y;

        double x2 = pt2.x;
        double y2 = pt2.y;

        double inner_product = x1*x2 + y1*y2;
        double len1 = Math.hypot(x1, y1);
        double len2 = Math.hypot(x2, y2);

        double a=Math.acos(inner_product/(len1*len2));
        return a*180/Math.PI;


    }

    //lineIntersect
    public double[] lineIntersect(double m1,double b1, double m2, double b2){
        double a_1=-m1;
        double b_1=1;
        double c_1=b1;

        double a_2=-m2;
        double b_2=1;
        double c_2=b2;

        double d = a_1*b_2-a_2*b_1; //determinant
        double dx = c_1*b_2-c_2*b_1;
        double dy = a_1*c_2-a_2*c_1;

        double intersectionX = dx/d;
        double intersectionY = dy/d;
        double val[] ={intersectionX,intersectionY};
        return val;

    }

    public void process(Mat rgbaImage) {

    /* Gray scale */
        mGray = new Mat(rgbaImage.size(), CvType.CV_8UC1);
        Imgproc.cvtColor(rgbaImage, mGray, Imgproc.COLOR_RGB2GRAY);

    /*Threshold 1 */
        mThresh1 = new Mat(mGray.size(), mGray.type());
        Imgproc.threshold(mGray, mThresh1, thresh, 255, 0);

        mEdges = new Mat(mThresh1.size(), mThresh1.type());

        Imgproc.Canny(mThresh1, mEdges, thresh, thresh * 2);



    /* findContours */

        Imgproc.findContours(mEdges, mContours, mHierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        /*** draw lines ***/

        Iterator<MatOfPoint> each = mContours.iterator();
        Scalar lineColor = new Scalar(0, 255, 0);

        while(each.hasNext()){
            MatOfPoint wrapper = each.next();
            Rect rect = Imgproc.boundingRect(wrapper);
            Imgproc.drawContours(mDrawing, mContours, 0, lineColor);

            if (rect.width > bw_width) {
                Imgproc.line(rgbaImage, new Point(rect.x, rect.y), new Point((rect.x + rect.width), (rect.y + rect.height)), lineColor);
                bxRight.add(rect.x + rect.width);
                byRight.add(rect.y + rect.height);
                bxLeft.add(rect.x);
                byLeft.add(rect.y);
                //int[] lArray = {rect.x, rect.y};
                bxbyLeftArray.add(new Point(rect.x, rect.y));
                //int[] rArray = {(rect.x + rect.width), (rect.y + rect.height)};
                bxbyRightArray.add(new Point((rect.x + rect.width), (rect.y + rect.height)));
                Scalar pinkCircle = new Scalar(0, 255, 255);

                Imgproc.circle(rgbaImage, new Point(rect.x, rect.y), 10, pinkCircle, 2);
                Imgproc.circle(rgbaImage, new Point((rect.x + rect.width), (rect.y + rect.height)), 10, pinkCircle, 2);


                //numRectangles++;
            }

        }

    }
//
//    /* Get medianL */
//        Collections.sort(bxLeft);
//        Collections.sort(byLeft);
//        Log.d(TAG, "bxLeft size " + bxLeft.size()/2);
//        Log.d(TAG, "byLeft size " + byLeft.size()/2);
//
//        Point MedianL = new Point(bxLeft.get((bxLeft.size()/2)), byLeft.get((byLeft.size()/2)));
//        Log.d(TAG, "Get MedianL");
//
//    /* Get MedianR */
//        Collections.sort(bxRight);
//        Collections.sort(byRight);
//        Point MedianR = new Point(bxRight.get((bxRight.size()/2)), byRight.get((byRight.size()/2)));
//        Log.d(TAG, "Get MedianR");
//
//    /* Draw Black Circle */
//        Scalar blackCircle = new Scalar(0, 0,0);
//        Imgproc.circle(rgbaImage, new Point(MedianL.x, MedianL.y), radius, blackCircle,2);
//        Imgproc.circle(rgbaImage, new Point(MedianR.x, MedianR.y), radius, blackCircle,2);
//        Log.d(TAG, "Draw Black Circle");
//
//        /*** Bounded within a Circle ***/
//        int cnt = 0;
//        java.util.List<Point>  boundedLeft = new ArrayList<>();
//        java.util.List<Point>  boundedRight = new ArrayList<>();
//        Log.d(TAG, "Bounded Circle 1");
//
//        for(Point i: bxbyLeftArray){
//            if((Math.pow((MedianL.x - i.x), 2) + Math.pow((MedianL.y - i.y), 2) < Math.pow(radius, 2))){
//                boundedLeft.add(i);
//                cnt++;
//            }
//        }
//        Log.d(TAG, "Bounded Circle 2");
//        cnt = 0;
//        for(Point i: bxbyRightArray){
//            if((Math.pow((MedianR.x - i.x), 2) + Math.pow((MedianR.y - i.y), 2) < Math.pow(radius, 2))){
//                boundedRight.add(i);
//                cnt++;
//            }
//        }
//        Log.d(TAG, "Bounded Circle 3");
//    /* Left Bound */
//        MatOfPoint boundedL = new MatOfPoint();
//        boundedL.fromList(boundedLeft);
//        Mat lLine = new Mat();
//        Imgproc.fitLine(boundedL, lLine, Imgproc.DIST_L2, 0, 0.01,0.01 );
//        Log.d(TAG, "BoundL");
//
//    /* Right Bound */
//        MatOfPoint boundedR = new MatOfPoint();
//        boundedR.fromList(boundedRight);
//        Mat rLine = new Mat();
//        Imgproc.fitLine(boundedR, rLine, Imgproc.DIST_L2, 0, 0.01,0.01 );
//        Log.d(TAG, "BoundR");
//
//        int m = radius*10;
//        Scalar redLine = new Scalar(255, 0,0);
//
//    /* Draw Lines */
//        Imgproc.line(rgbaImage, new Point((lLine.cols() - lLine.width()*m), (lLine.rows()-m*lLine.height())),
//                new Point((rLine.cols() + rLine.width()*m), (lLine.rows()+m*lLine.height())),
//                redLine, 3);
//        Log.d(TAG, "Draw Lines 1");
//
//        Imgproc.line(rgbaImage, new Point((rLine.cols() - rLine.width()*m), (rLine.rows()-m*rLine.height())),
//                new Point((rLine.cols() + rLine.width()*m), (rLine.rows()+m*rLine.height())),
//                redLine, 3);
//        Log.d(TAG, "Draw Lines 2");
//
//    /* Convert back to bitMap */
//        Utils.matToBitmap(rgbaImage, result);
//        Log.d(TAG, "processed -> True");
//
//        return true;
//    }


}
