package org.opencv.samples.seek;


import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.openimaj.math.model.fit.RANSAC;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Created by dobi on 4/12/17.
 */

public class CrossWalkDetector{

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
    java.util.List<Double> Bdist = new ArrayList<>();


    java.util.List<Integer> bxLeft = new ArrayList<>();
    java.util.List<Integer> byLeft = new ArrayList<>();
    java.util.List<Integer> bxRight = new ArrayList<>();
    java.util.List<Integer> byRight = new ArrayList<>();

    //lineCalc
//    public double[] lineCalc(double vx, double vy, double x0, double y0 ){
//        double scale =10;
//        double x1 = x0+scale*vx;
//        double y1 = y0+scale*vy;
//        double m = (y1-y0)/(x1-x0);
//        double b = y1-m*x1;
//
//        double val[] = {m,b};
//        return val;
//
//    }

//    //angle
//    public double angle(Point pt1, Point pt2){
//        double x1 = pt1.x;
//        double y1 = pt1.y;
//
//        double x2 = pt2.x;
//        double y2 = pt2.y;
//
//        double inner_product = x1*x2 + y1*y2;
//        double len1 = Math.hypot(x1, y1);
//        double len2 = Math.hypot(x2, y2);
//
//        double a=Math.acos(inner_product/(len1*len2));
//        return a*180/Math.PI;
//
//    }

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
//    /* returns a list of median distance from a list of points */
//    private double xdiff(java.util.List<Point>  input){
//        java.util.List<Double> xvals = new ArrayList<>();
//
//        Log.d(TAG,"calculating median");
//        for (int i = 0; i < input.size() ; i++) {
//            xvals.add(input.get(i).x);
//        }
//        Collections.sort(xvals);
//        double median = xvals.get(xvals.size()/2);
//
//        return median;
//    }
//    /* returns a list of median distance from a list of points */
//    private double med(java.util.List<Double>  input){
//        Log.d(TAG,"calculating median");
//        Collections.sort(input);
//        double median = input.get(input.size()/2);
//
//        return median;
//    }
//    /* returns a average from a list of points */
//    private Point avg(java.util.List<Point> input){
//        Log.d(TAG,"calculating average");
//        int sumx = 0;
//        int sumy = 0;
//        for (int i = 0; i < input.size(); i++) {
//            sumx += input.get(i).x;
//            sumy += input.get(i).y;
//        }
//        double x = sumx/input.size();
//        double y = sumy/input.size();
//        Log.d(TAG,"Point Average " + "("+ x + ", "+ y + ")");
//
//        return new Point(x,y);
//    }
    /* returns a average from a list of points */
    private double avgx(java.util.List<Point> input){
        Log.d(TAG,"calculating average");
        int sumx = 0;
        for (int i = 0; i < input.size(); i++) {
            sumx += input.get(i).x;
        }
        double x = sumx/input.size();
        return x;
    }
    /* returns a average from a list of points */
//    private Point avg2(java.util.List<Point> inputL, java.util.List<Point> inputR){
//        Log.d(TAG,"calculating average of all the lengths");
//        int sumx = 0;
//        int sumy = 0;
//        for (int i = 0; i < inputL.size(); i++) {
//            sumx += inputL.get(i).x;
//            sumy += input.get(i).y;
//        }
//        double x = sumx/input.size();
//        double y = sumy/input.size();
//        Log.d(TAG,"Point Average " + "("+ x + ", "+ y + ")");
//
//        return new Point(x,y);
//    }
    /* returns a list of closely related points */
//    private java.util.List crt(java.util.List<Point> input){
//        Log.d(TAG,"CRT");
//        java.util.List<Point> Output = new ArrayList<>();
//        Point avg = avg(input);
//        double x1 = avg.x;
//        double y1 = avg.y;
//
//
//        for (int i = 0; i < input.size(); i++) { //7 random points
//            double x2 = input.get(i).x;
//            double y2 = input.get(i).y;
//            Log.d(TAG,"Point " + "("+ x2 + ", "+ y2 + ")");
//            double distance = Math.sqrt(Math.pow((x2-x1),2) + Math.pow((y2-y1),2));
//            Log.d(TAG,"Distance is: "+ distance);
//            if(distance <= 950){
//                Output.add(input.get(i));
//            }
//        }
//        Log.d(TAG, "Output size: "+Output.size());
//        return Output;
//    }
//    /* returns a list of closely related points */
//    private java.util.List<java.util.List<Point>> crt3(Mat img, java.util.List<Point> leftBound, java.util.List<Point> rightBound, java.util.List<Double>  distList){
//        Log.d(TAG,"CRT2");
//        java.util.List<Point> outputL = new ArrayList<>();
//        java.util.List<Point> outputR = new ArrayList<>();
//
//        int medDist = (int)med(distList);
//        Log.d(TAG,"Median Dist: " + medDist);
//        for (int i = 0; i < leftBound.size(); i++) { //7 random points
//            double x1 = leftBound.get(i).x;
//            double y1 = leftBound.get(i).y;
//            double x2 = rightBound.get(i).x;
//            double y2 = rightBound.get(i).y;
//            Log.d(TAG,"Point1 " + "("+ x1 + ", "+ y1 + ")");
//            Log.d(TAG,"Point2 " + "("+ x2 + ", "+ y2 + ")");
//            double dist = Math.sqrt(Math.pow((x2-x1),2) + Math.pow((y2-y1),2));
//            Log.d(TAG,"Point Dist: "+ dist);
//            if((int)dist >= medDist - (medDist/4) && (int)dist <= medDist + (medDist/4)){
//                Log.d(TAG,"KEPT: "+ dist);
//                outputL.add(leftBound.get(i));
//                outputR.add(rightBound.get(i));
//                Imgproc.circle(img, leftBound.get(i), 10, new Scalar(100,100,250), 2); //left points
//                Imgproc.circle(img, rightBound.get(i), 10,  new Scalar(250,250,250), 2); //right points
//            }
//        }
//        java.util.List<java.util.List<Point>> list = new ArrayList<>();
//        list.add(outputL);
//        list.add(outputR);
//        return list;
//    }
//    /* returns a list of closely related points */
//    private java.util.List<java.util.List<Point>> crt2(Mat img, java.util.List<Point> leftBound, java.util.List<Point> rightBound, int avgdist){
//        Log.d(TAG,"CRT2");
//        java.util.List<Point> outputL = new ArrayList<>();
//        java.util.List<Point> outputR = new ArrayList<>();
//
//        for (int i = 0; i < leftBound.size(); i++) { //7 random points
//            double x1 = leftBound.get(i).x;
//            double y1 = leftBound.get(i).y;
//            double x2 = rightBound.get(i).x;
//            double y2 = rightBound.get(i).y;
//            Log.d(TAG,"Point1 " + "("+ x1 + ", "+ y1 + ")");
//            Log.d(TAG,"Point2 " + "("+ x2 + ", "+ y2 + ")");
//            double dist = Math.sqrt(Math.pow((x2-x1),2) + Math.pow((y2-y1),2));
//            Log.d(TAG,"Point Dist: "+ dist);
//            if((int)dist >= avgdist + (avgdist/6)){
//                outputL.add(leftBound.get(i));
//                outputR.add(rightBound.get(i));
//                Imgproc.circle(img, leftBound.get(i), 10, new Scalar(100,100,250), 2); //left points
//                Imgproc.circle(img, rightBound.get(i), 10,  new Scalar(250,250,250), 2); //right points
//            }
//        }
//        java.util.List<java.util.List<Point>> list = new ArrayList<>();
//        list.add(outputL);
//        list.add(outputR);
//        return list;
//    }
//    /* returns a list of closely related points */
//    private java.util.List<java.util.List<Point>> crt4(Mat img, java.util.List<Point> leftBound, java.util.List<Point> rightBound){
//        Log.d(TAG,"CRT4");
//        java.util.List<Point> outputL = new ArrayList<>();
//        java.util.List<Point> outputR = new ArrayList<>();
//
//        int x1med = (int)xdiff(leftBound);
//        int x2med = (int)xdiff(rightBound);
//        Log.d(TAG,"x1med: " + x1med);
//        Log.d(TAG,"x2med: " + x2med);
//
//        for (int i = 0; i < leftBound.size(); i++) { //7 random points
//            double x1 = leftBound.get(i).x;
//            double y1 = leftBound.get(i).y;
//            double x2 = rightBound.get(i).x;
//            double y2 = rightBound.get(i).y;
//            Log.d(TAG,"Point1 " + "("+ x1 + ", "+ y1 + ")");
//            Log.d(TAG,"Point2 " + "("+ x2 + ", "+ y2 + ")");
//            //double dist = Math.sqrt(Math.pow((x2-x1),2) + Math.pow((y2-y1),2));
//            //Log.d(TAG,"Point Dist: "+ dist);
//            if((x1 >= (x1med - (x1med/5))) && (x1 <= (x1med + (x1med/5))) && (x2 >= (x2med - (x2med/5))) && (x2 <= (x2med + (x2med/5)))  ){
//                Log.d(TAG,"KEPT: Point1 " + "("+ x1 + ", "+ y1 + ")");
//                Log.d(TAG,"KEPT: Point2 " + "("+ x2 + ", "+ y2 + ")");
//                outputL.add(leftBound.get(i));
//                outputR.add(rightBound.get(i));
//                Imgproc.circle(img, leftBound.get(i), 15, new Scalar(244,107,66), 3); //left points
//                Imgproc.circle(img, rightBound.get(i), 15,  new Scalar(250,250,250), 3); //right points
//            }
//        }
//        java.util.List<java.util.List<Point>> list = new ArrayList<>();
//        list.add(outputL);
//        list.add(outputR);
//        return list;
//    }
//    /* returns a list of closely related points */
    private java.util.List<java.util.List<Point>> crt5(Mat img, java.util.List<Point> leftBound, java.util.List<Point> rightBound){
        Log.d(TAG,"CRT4");
        java.util.List<Point> outputL = new ArrayList<>();
        java.util.List<Point> outputR = new ArrayList<>();

        int x1avg = (int)avgx(leftBound);
        int x2avg = (int)avgx(rightBound);
        Log.d(TAG,"x1avg: " + x1avg);
        Log.d(TAG,"xavg: " + x2avg);

        for (int i = 0; i < leftBound.size(); i++) { //7 random points
            double x1 = leftBound.get(i).x;
            double y1 = leftBound.get(i).y;
            double x2 = rightBound.get(i).x;
            double y2 = rightBound.get(i).y;
            Log.d(TAG,"Point1 " + "("+ x1 + ", "+ y1 + ")");
            Log.d(TAG,"Point2 " + "("+ x2 + ", "+ y2 + ")");
            //double dist = Math.sqrt(Math.pow((x2-x1),2) + Math.pow((y2-y1),2));
            //Log.d(TAG,"Point Dist: "+ dist);
            if((x1 >= (x1avg - (x1avg/5))) && (x1 <= (x1avg + (x1avg/5))) && (x2 >= (x2avg - (x2avg/5))) && (x2 <= (x2avg + (x2avg/5)))  ){
                Log.d(TAG,"KEPT: Point1 " + "("+ x1 + ", "+ y1 + ")");
                Log.d(TAG,"KEPT: Point2 " + "("+ x2 + ", "+ y2 + ")");
                outputL.add(leftBound.get(i));
                outputR.add(rightBound.get(i));
                Imgproc.circle(img, leftBound.get(i), 15, new Scalar(244,107,66), 3); //left points
                Imgproc.circle(img, rightBound.get(i), 15,  new Scalar(250,250,250), 3); //right points
            }
        }
        java.util.List<java.util.List<Point>> list = new ArrayList<>();
        list.add(outputL);
        list.add(outputR);
        return list;
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
        //java.util.List<Point> outputLine = new ArrayList<>();
        Mat outLine = new Mat(); //Right bounding line
        Log.d(TAG, "bLine size: "+bLine.size());
        //matOfPoint.fromList(setRand(bLine)); //mat from random points
        //outputLine = crt(bLine);
        //matOfPoint.fromList(crt(bLine)); //mat from random points
//        if (outputLine.size() < 2){
//            //matOfPoint.fromList(bLine); //mat from all points
//            double[] empty = {};
//            return empty;
//
//        }
//        matOfPoint.fromList(outputLine); //mat from random points

        matOfPoint.fromList(bLine); //mat from all points

        /*relase bLine */
        Imgproc.fitLine(matOfPoint, outLine, Imgproc.DIST_L2, 0,0.5,0.5);

        double[] p_1 = outLine.get(0,0);//normalized direction x
        double[] p_2 = outLine.get(1,0);//normalized direction y
        double[] p_3 = outLine.get(2,0);//point on line x
        double[] p_4 = outLine.get(3,0);//point on line y
        double LTlineL = (-p_3[0]) * (p_1[0]/p_2[0]) + p_4[0];
        double LTlineR = (img.cols() - p_3[0]) * (p_1[0]/p_2[0]) + p_4[0];

        double slope = (LTlineL-LTlineR)/(0-img.cols());
        double intercept = LTlineR - (slope * img.cols());
        double[] slope_intercept = {slope, intercept};

        return slope_intercept;
    }
//    public void _RANSAC(java.util.List<Point> input){
//        RANSAC model_ransac = new RANSAC(new MultipleLinearRegression(), 100, input.size(), 7, 5 );
//
//        //convert input to data
//        //RegressionDataSet train;
//        //train data set
//        //return the model that yielded the best test
//
//        //get the consensus
//
//        //for loop the boolean consensus set
//
//        //convert back to Opencv points
//
//        //return a java.util.List<Point>
//    }

    public void _RANSAC(java.util.List<Point> input){
        //create a ransac instance
        RANSAC model_ransac;

        //fit the data to the ransac instance

        //next after fitting is done get in inliers

        //return the pair of inliers


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
        Scalar yellowCircle = new Scalar(255, 255, 0);
        Scalar lineColor = new Scalar(0, 255, 0);
        Iterator<MatOfPoint> each = contours.iterator();
        MatOfPoint wrapper;
        Rect rect;
        while(each.hasNext()){
            wrapper = each.next();
            rect = Imgproc.boundingRect(wrapper);
            //if (rect.width > bw_width){

            if (rect.width >= rgbaImage.width() - (rgbaImage.width()*.75)){ //rect with should be 1/4 or greater then the screen width
                Point LBP = new Point(rect.x, rect.y); //left bound point
                Point RBP = new Point((rect.x + rect.width), rect.y); //right bound point
                bxbyLeftArray.add(LBP); //points for left line segment
                bxbyRightArray.add(RBP); //points for right lines segment

                //double dist = Math.sqrt(Math.pow((RBP.x-LBP.x),2) + Math.pow((RBP.y-LBP.y),2));
               // Bdist.add(dist);
                Imgproc.line(rgbaImage, new Point(rect.x, rect.y), new Point((rect.x + rect.width), rect.y), lineColor,2);
                Imgproc.circle(rgbaImage, new Point(rect.x, rect.y), 10, yellowCircle, 4);
                Imgproc.circle(rgbaImage, new Point((rect.x + rect.width), rect.y), 10, tealCircle, 4);
            }
        }

        if(bxbyLeftArray.size() >=7){
            Scalar boundColor = new Scalar(0, 0, 255);
            Scalar intersectColor = new Scalar(255,0,0);
            //Calculate average of the distance
//            int sum = 0;
//            for (int i = 0; i <Bdist.size() ; i++) {
//                sum += Bdist.get(i);
//            }
            ///int avgDist = sum/Bdist.size();
            //Log.d(TAG,"avgDist is: "+avgDist);
            //call crt2 to remove points that don't match dist
            //java.util.List<java.util.List<Point>> bounds= crt2(rgbaImage, bxbyLeftArray,bxbyRightArray, avgDist);

            //call crt3
            //java.util.List<java.util.List<Point>> bounds= crt3(rgbaImage, bxbyLeftArray,bxbyRightArray, Bdist);
            //java.util.List<java.util.List<Point>> bounds= crt4(rgbaImage, bxbyLeftArray,bxbyRightArray);
            java.util.List<java.util.List<Point>> bounds= crt5(rgbaImage, bxbyLeftArray,bxbyRightArray);

            double[] LTline = setBounds(rgbaImage, bounds.get(0)); //line boundary for left points
            double[] RTline = setBounds(rgbaImage, bounds.get(1)); //line boundary for right points
            if (LTline.length > 1 && RTline.length > 1) {


                double m1 = LTline[0]; //slope left line
                double b1 = LTline[1]; //intersect right line
                double m2 = RTline[0]; //slope left line
                double b2 = RTline[1]; //intersect right line

                Imgproc.line(rgbaImage, new Point((0 - b1) / m1, 0), new Point((rgbaImage.height() - b1) / m1, rgbaImage.height()), boundColor, 3);
                Imgproc.line(rgbaImage, new Point((0 - b2) / m2, 0), new Point((rgbaImage.height() - b2) / m2, rgbaImage.height()), boundColor, 3);

                double[] intersect = lineIntersect(m2, b2, m1, b1);
                Imgproc.circle(rgbaImage, new Point((int) intersect[0], (int) intersect[1]), 10, intersectColor, 15);
            }

        }
    }
}