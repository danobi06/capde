package org.opencv.samples.seek;

import android.graphics.Bitmap;

import android.widget.ImageView;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by dobi on 4/12/17.
 */

public class CrossWalkDetector {


    /* Local Variables */
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


            }

        }

    }

    public List<MatOfPoint> getContours() {
        return mContours;
    }
}
