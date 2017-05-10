package org.opencv.samples.seek;

import android.util.Log;

import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by dobi on 4/12/17.
 */

public class CrossWalkDetector{

    private static String TAG = "CrossDetector";

    /* Local Variables */
    double thresh = 150; //optimal 150
    int radius = 150;
    long count = 0;
    //int bw_width = 300; //#170

    //Cache
    //Mat rgbaImage = new Mat(); //image to process
    Mat mGray = new Mat(); //grey Scale
    Mat mBlur = new Mat(); //gaussian blur
    Mat mMask = new Mat(); //mask
    Mat th2 = new Mat(); //threshold
    Mat erode = new Mat();
//    Mat horizontal = new Mat();



    Mat mThresh1 = new Mat();
    Mat mEdges = new Mat();
    Mat mHierarchy = new Mat();
//
//    java.util.List<Integer> bxLeft = new ArrayList<>();
//    java.util.List<Integer> byLeft = new ArrayList<>();
//    java.util.List<Integer> bxRight = new ArrayList<>();
//    java.util.List<Integer> byRight = new ArrayList<>();

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

//    /* returns a list of random points */
//    private java.util.List setRand(java.util.List<Point> input){
//        Log.d(TAG,"randomizing boundaries");
//        java.util.List<Point> randOutput = new ArrayList<>();
//
//        for (int i = 0; i < input.size()/2; i++) { //7 random points
//            randOutput.add(input.get(new Random().nextInt(input.size())));
//        }
//
//        return randOutput;
//    }
    /* sets the bounds for the crosswalk */
    private double[] setBounds(Mat img, java.util.List<Point> bLine){
        MatOfPoint matOfPoint = new MatOfPoint();
        //java.util.List<Point> outputLine = new ArrayList<>();
        Mat outLine = new Mat(); //Right bounding line

        matOfPoint.fromList(bLine); //mat from all points

        /*relase bLine */
        Imgproc.fitLine(matOfPoint, outLine, Imgproc.DIST_L2, 0,0.01,0.01);


        double[] p_1 = outLine.get(0,0);//normalized direction x
        double[] p_2 = outLine.get(1,0);//normalized direction y
        double[] p_3 = outLine.get(2,0);//point on line x
        double[] p_4 = outLine.get(3,0);//point on line y

//        Log.d(TAG,"Outline size: "+outLine.size());
//
//        Log.d(TAG,"Direction X: "+p_1[0]);
//        Log.d(TAG,"Direction X size: "+p_1.length);
//
//        Log.d(TAG,"Direction Y: "+p_2[0]);
//        Log.d(TAG,"Direction Y size: "+p_2.length);
//
//        Log.d(TAG,"point on line x: "+p_3[0]);
//        Log.d(TAG,"point on line x size: "+p_3.length);
//
//        Log.d(TAG,"Point on line Y: "+p_4[0]);
//        Log.d(TAG,"Point on line Y size: "+p_4.length);
//
        double vals[] = {p_1[0],p_2[0],p_3[0],p_4[0]};

        return vals;
    }
    public java.util.List<Point> toPointCV(java.util.List<Point2D> dogPoints){
        java.util.List<Point> cvPoint = new ArrayList<>();
        for (int i = 0; i < dogPoints.size(); i++) {
            cvPoint.add(new Point(dogPoints.get(i).x,dogPoints.get(i).y));
        }
        return cvPoint;
    }

    public java.util.List<Point2D> toPoint2D(java.util.List<Point> cvPoint){
        java.util.List<Point2D> dogPoint = new ArrayList<>();
        for (int i = 0; i < cvPoint.size(); i++) {
            dogPoint.add(new Point2D(cvPoint.get(i).x,cvPoint.get(i).y));
        }
        return dogPoint;
    }
    public java.util.List<Point> _RANSAC(java.util.List<Point> input){

        List<Point2D> points = toPoint2D(input);

        //------------------------ Compute the solution
        // Let it know how to compute the model and fit errors
        ModelManager<Line2D> manager = new LineManager();
        ModelGenerator<Line2D,Point2D> generator = new LineGenerator();
        DistanceFromModel<Line2D,Point2D> distance = new DistanceFromLine();

        // RANSAC or LMedS work well here
        ModelMatcher<Line2D,Point2D> alg =
                new Ransac<Line2D,Point2D>(234234,manager,generator,distance,200,10);

        if( !alg.process(points) )
            throw new RuntimeException("Robust fit failed!");
        // let's look at the results
        Line2D found = alg.getModelParameters();

        return toPointCV(alg.getMatchSet());
    }

    public void process(Mat rgbaImage) {
        java.util.List<Point> bxbyLeftArray = new ArrayList<>();
        java.util.List<Point> bxbyRightArray = new ArrayList<>();

//  /* Gray scale */
        Imgproc.cvtColor(rgbaImage, mGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(mGray, mBlur,new Size(5,5),0);
        mGray.release();
        Core.inRange(mBlur, new Scalar(170,170,170), new Scalar(255,255,255), mMask);
        mBlur.release();
////
//////    /*Threshold 1 */
////        Imgproc.threshold(mGray, mThresh1, thresh, 255, 0);
////        mGray.release();
////
////        Imgproc.Sobel(mThresh1, mEdges, CvType.CV_8U, 0,1);
////
//////    /* findContours */
//        List<MatOfPoint> contours = new ArrayList<>();
////        /* TODO
////         limit contour size */
//        Imgproc.findContours(mEdges, contours, mHierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
//        mEdges.release();
//


//        Imgproc.adaptiveThreshold(mMask, th2, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C,Imgproc.THRESH_BINARY,15,-2);
        Log.d(TAG,"copied threshold");

        int cols = mMask.cols();

        int horizontalsize = cols / 30;

        Mat horizontalStructure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(horizontalsize, 1));
        Log.d(TAG,"got structuring element: "+ horizontalStructure);

        Imgproc.erode(mMask, erode, horizontalStructure);
        mMask.release();

//        Imgproc.Sobel(mMask, mEdges, CvType.CV_8U, 0,1);

        List<MatOfPoint> contours = new ArrayList<>();

        Imgproc.findContours(erode, contours, mHierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        Log.d(TAG,"found contours");
        erode.release();

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

            if (rect.width >= rgbaImage.width() - (rgbaImage.width()*.75)){ //rect with should be 1/4 or greater then the screen width
                Point LBP = new Point(rect.x, rect.y); //left bound point
                Point RBP = new Point((rect.x + rect.width), rect.y); //right bound point
                bxbyLeftArray.add(LBP); //points for left line segment
                bxbyRightArray.add(RBP); //points for right lines segment

                Imgproc.line(rgbaImage, new Point(rect.x, rect.y), new Point((rect.x + rect.width), rect.y), lineColor,2);
                Imgproc.circle(rgbaImage, new Point(rect.x, rect.y), 10, yellowCircle, 4);
                Imgproc.circle(rgbaImage, new Point((rect.x + rect.width), rect.y), 10, tealCircle, 4);
            }
        }

        if(bxbyLeftArray.size() >=7){
            Scalar boundColor = new Scalar(0, 0, 255);
            Scalar intersectColor = new Scalar(255,0,0);

            java.util.List<Point> inlierL = _RANSAC(bxbyLeftArray);
            Log.d(TAG,"inlierLSize: " + inlierL.size());
            java.util.List<Point> inlierR = _RANSAC(bxbyRightArray);
            Log.d(TAG,"inlierRSize: " + inlierR.size());


            for (int i = 0; i < inlierL.size(); i++) {
                Imgproc.circle(rgbaImage, inlierL.get(i), 15, new Scalar(244,107,66), 3); //left points

                    Log.d(TAG,"InlierL " + "("+ inlierL.get(i).x + ", "+ inlierL.get(i).y + ")");
            }
            for (int i = 0; i < inlierR.size(); i++) {
                Imgproc.circle(rgbaImage, inlierR.get(i), 15, new Scalar(250,250,250), 3); //right points
                Log.d(TAG,"InlierR " + "("+ inlierR.get(i).x + ", "+ inlierR.get(i).y + ")");

            }

            //double[] LTline = setBounds(rgbaImage, bounds.get(0)); //line boundary for left points
            //double[] RTline = setBounds(rgbaImage, bounds.get(1)); //line boundary for right points
            double[] LTline = setBounds(rgbaImage, inlierL); //line boundary for left points
            double[] RTline = setBounds(rgbaImage, inlierR); //line boundary for right points
            if (LTline.length > 1 && RTline.length > 1) {

//                Log.d(TAG,"about to get bounds ");
                double vx = LTline[0];
                double vy = LTline[1];
                double x0 = LTline[2];
                double y0 = LTline[3];
//                Log.d(TAG,"Left Bound Values ");


                double vx_R = RTline[0];
                double vy_R = RTline[1];
                double x0_R = RTline[2];
                double y0_R = RTline[3];
//                Log.d(TAG,"Right Bound Values ");

                int m = radius*10;

//                Log.d(TAG,"drawing bounds");

                Imgproc.line(rgbaImage, new Point(x0-m*vx, y0-m*vy), new Point(x0+m*vx, y0+m*vy), boundColor, 3);
                Imgproc.line(rgbaImage, new Point(x0_R-m*vx_R, y0_R-m*vy_R), new Point(x0_R+m*vx_R, y0_R+m*vy_R), boundColor, 3);

                double mbL[] =lineCalc(vx, vy, x0, y0);

                double mbR[] =lineCalc(vx_R, vy_R, x0_R, y0_R);

                double[] intersect = lineIntersect(mbR[0], mbR[1], mbL[0], mbL[1]);

                Imgproc.circle(rgbaImage, new Point((int) intersect[0], (int) intersect[1]), 10, intersectColor, 15);
                //count++;
                //Log.d(TAG, "Count Num: " + count);
            }

        }
    }
}