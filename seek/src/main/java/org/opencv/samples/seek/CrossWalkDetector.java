package org.opencv.samples.seek;


import android.util.Log;

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
import java.util.Random;

/**
 * Created by dobi on 4/12/17.
 */

public class CrossWalkDetector {

    private static String TAG = "CrossDetector";

    /* Local Variables */
    double thresh = 150; //optimal 150
    int bw_width = 300; //#170

    //Cache
    //Mat rgbaImage = new Mat(); //image to process
    Mat mGray = new Mat(); //grey Scale
    Mat mThresh1 = new Mat();
    Mat mEdges = new Mat();
    Mat mHierarchy = new Mat();


    java.util.List<Integer> bxLeft = new ArrayList<>();
    java.util.List<Integer> byLeft = new ArrayList<>();
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

        double d = (a_1*b_2)-(a_2*b_1); //determinant
        double dx = (c_1*b_2)-(c_2*b_1);
        double dy = (a_1*c_2)-(a_2*c_1);

        double intersectionX = dx/d;
        double intersectionY = dy/d;
        double val[] ={intersectionX,intersectionY};
        return val;

    }
    /* returns a list of random points */
    private java.util.List setRand(java.util.List<Point> input){
        Log.d(TAG,"randomizing boundaries");
        java.util.List<Point> randOutput = new ArrayList<>();

        for (int i = 0; i < input.size()/2; i++) { //7 random points
            randOutput.add(input.get(new Random().nextInt(input.size())));
        }

        return randOutput;
    }
    /* sets the bounds for the crosswalk */
    private double[] setBounds(Mat img, java.util.List<Point> bLine){
        MatOfPoint matOfPoint = new MatOfPoint();
        Mat outLine = new Mat(); //Right bounding line
        Log.d(TAG, "bLine size: "+bLine.size());
        //matOfPoint.fromList(setRand(bLine)); //mat from random points
        matOfPoint.fromList(bLine); //mat from all points

        /*relase bLine */
        Imgproc.fitLine(matOfPoint, outLine, Imgproc.DIST_L2, 0,0,0.01);

        double[] p_1 = outLine.get(0,0);//normalized direction x
        double[] p_2 = outLine.get(1,0);//normalized direction y
        double[] p_3 = outLine.get(2,0);//point on line x
        double[] p_4 = outLine.get(3,0);//point on line y
        double LTlineL = (-p_3[0]) * (p_1[0]/p_2[0]) + p_4[0];
        double LTlineR = (img.cols() - p_3[0]) * (p_1[0]/p_2[0]) + p_4[0];
        double[] pair = {LTlineL, LTlineR};
        return pair;
    }

    public void process(Mat rgbaImage) {
        java.util.List<Point> bxbyLeftArray = new ArrayList<>();
        java.util.List<Point> bxbyRightArray = new ArrayList<>();

  /* Gray scale */
        Imgproc.cvtColor(rgbaImage, mGray, Imgproc.COLOR_RGB2GRAY);

    /*Threshold 1 */
        Imgproc.threshold(mGray, mThresh1, thresh, 255, 0);
        mGray.release();

        Imgproc.Sobel(mThresh1, mEdges, CvType.CV_8U, 0,1);

    /* findContours */
        List<MatOfPoint> contours = new ArrayList<>();
        /* TODO
         limit contour size */
        Imgproc.findContours(mEdges, contours, mHierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        mEdges.release();
        /*** draw lines ***/
        Scalar tealCircle = new Scalar(0, 255, 255);
        Scalar lineColor = new Scalar(0, 255, 0);
        Iterator<MatOfPoint> each = contours.iterator();
        MatOfPoint wrapper;
        Rect rect;
        while(each.hasNext()){
            wrapper = each.next();
            rect = Imgproc.boundingRect(wrapper);

            if (rect.width > bw_width){
//                (rect.y + rect.height)), lineColor);
//                bxRight.add(rect.x + rect.width);
//                byRight.add(rect.y);
//                bxLeft.add(rect.x);
//                byLeft.add(rect.y);
                bxbyLeftArray.add(new Point(rect.x, rect.y)); //points for left line segment
                bxbyRightArray.add(new Point((rect.x + rect.width), rect.y)); //points for right lines segment
                Imgproc.line(rgbaImage, new Point(rect.x, rect.y), new Point((rect.x + rect.width), rect.y), lineColor,2);
                Imgproc.circle(rgbaImage, new Point(rect.x, rect.y), 10, tealCircle, 2);
                Imgproc.circle(rgbaImage, new Point((rect.x + rect.width), rect.y), 10, tealCircle, 2);
            }
        }

        if(bxbyLeftArray.size() >=5){
            Scalar boundColor = new Scalar(0, 0, 255);
            Scalar intersectColor = new Scalar(255,0,0);
            double[] LTline = setBounds(rgbaImage, bxbyLeftArray); //line boundary for left points
            double[] RTline = setBounds(rgbaImage, bxbyRightArray); //line boundary for right points
            Imgproc.line(rgbaImage, new Point(rgbaImage.cols() - 1, LTline[1]), new Point(0, LTline[0]), boundColor, 3);
            Imgproc.line(rgbaImage, new Point(rgbaImage.cols() - 1, RTline[1]),new Point(0, RTline[0]), boundColor, 3);

            double m1 = (LTline[0]-LTline[1])/(0-rgbaImage.cols()); //slope left line
            double b1 = LTline[1] - m1*rgbaImage.cols(); //intersect right line
            double m2 = (RTline[0]-RTline[1])/(0-rgbaImage.cols()); //slope left line
            double b2 = RTline[1] - m2*rgbaImage.cols(); //intersect right line

            double[] intersect = lineIntersect(m2,b2, m1, b1);
            Imgproc.circle(rgbaImage, new Point ((int) intersect[0],(int) intersect[1]),10,intersectColor,15);
        }

    }
}