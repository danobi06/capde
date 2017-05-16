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

    /* Variables for  direction vector*/
    java.util.List<Integer>  DxArray= new ArrayList<>();
    java.util.List<Integer>  DyArray= new ArrayList<>();

    int after =0;
    int DxAve =0;
    int Dxold =0;
    int DyAve =0;
    int Dyold =0;
    int count = 0; //counts the number of times processed is called
    String state = "";


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
    public int sum(java.util.List<Integer> array){
        int sum = 0;
        for (int i = 0; i < array.size(); i++) {
            sum += array.get(i);
        }
        return sum;
    }
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
        /* Initialization */
        int W = rgbaImage.width();
        int H = rgbaImage.height();

//       int ratio = H/W;
//        W = 800;
//        H = W * ratio;

        java.util.List<Point> bxbyLeftArray = new ArrayList<>();
        java.util.List<Point> bxbyRightArray = new ArrayList<>();

//  /* Gray scale */
        Imgproc.cvtColor(rgbaImage, mGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(mGray, mBlur,new Size(5,5),0);
        mGray.release();
        Core.inRange(mBlur, new Scalar(170,170,170), new Scalar(255,255,255), mMask);
        mBlur.release();

//        Imgproc.adaptiveThreshold(mMask, th2, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C,Imgproc.THRESH_BINARY,15,-2);
       // Log.d(TAG,"copied threshold");

        int cols = mMask.cols();

        int horizontalsize = cols / 30;

        Mat horizontalStructure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(horizontalsize, 1));
        //Log.d(TAG,"got structuring element: "+ horizontalStructure);

        Imgproc.erode(mMask, erode, horizontalStructure);
        mMask.release();

//        Imgproc.Sobel(mMask, mEdges, CvType.CV_8U, 0,1);

        List<MatOfPoint> contours = new ArrayList<>();

        Imgproc.findContours(erode, contours, mHierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
       // Log.d(TAG,"found contours");
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
        contours.clear();

        if(bxbyLeftArray.size() >=3 && bxbyRightArray.size() >= 3){
            Scalar boundColor = new Scalar(0, 0, 255);
            Scalar intersectColor = new Scalar(255,0,0);

            java.util.List<Point> inlierL = _RANSAC(bxbyLeftArray);
            //Log.d(TAG,"inlierLSize: " + inlierL.size());
            java.util.List<Point> inlierR = _RANSAC(bxbyRightArray);
            //Log.d(TAG,"inlierRSize: " + inlierR.size());


            for (int i = 0; i < inlierL.size(); i++) {
                Imgproc.circle(rgbaImage, inlierL.get(i), 15, new Scalar(244,107,66), 3); //left points

                    //Log.d(TAG,"InlierL " + "("+ inlierL.get(i).x + ", "+ inlierL.get(i).y + ")");
            }
            for (int i = 0; i < inlierR.size(); i++) {
                Imgproc.circle(rgbaImage, inlierR.get(i), 15, new Scalar(250,250,250), 3); //right points
                //Log.d(TAG,"InlierR " + "("+ inlierR.get(i).x + ", "+ inlierR.get(i).y + ")");

            }

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

                int m = radius * 10;

                double mbL[] = lineCalc(vx, vy, x0, y0);

                double mbR[] = lineCalc(vx_R, vy_R, x0_R, y0_R);

                double[] intersect = lineIntersect(mbR[0], mbR[1], mbL[0], mbL[1]);

                if (intersect[1] < rgbaImage.height() / 2) {

                    Imgproc.line(rgbaImage, new Point(x0 - m * vx, y0 - m * vy), new Point(x0 + m * vx, y0 + m * vy), boundColor, 3);
                    Imgproc.line(rgbaImage, new Point(x0_R - m * vx_R, y0_R - m * vy_R), new Point(x0_R + m * vx_R, y0_R + m * vy_R), boundColor, 3);
                    Imgproc.circle(rgbaImage, new Point((int) intersect[0], (int) intersect[1]), 10, intersectColor, 15);
                }


                /* calculating the direction vector */

                int POVx = W / 2;
                int POVy = H / 2;

                int dx = -(int)(intersect[0] - POVx); //regular x,y axis coordinates
                int dy = -(int)(intersect[1] - POVy); //regular x,y axis coordinates

                Log.d(TAG,"intersect(" + intersect[0] + "," + intersect[1]+")");

                Log.d(TAG,"Count: : " + count);

                //int focalpx = (int) (W * 4.26 / 6.604);

                if (count < 6) {
                    DxArray.add(dx);
                    DyArray.add(dy);
                    count++;
                }
                else {
                    DxAve = sum(DxArray) / DxArray.size();
                    DyAve = sum(DyArray) / (DyArray.size());
                    DxArray.clear();
                    DyArray.clear();
                    count = 0;
                }

                Log.d(TAG,"intersect(" + intersect[0] + "," + intersect[1]+")");

                Log.d(TAG,"DxAve: " + DxAve + ", DyAve: " + DyAve);

                if ((DyAve > 30) && (Math.abs(DxAve) < 300)) {
                    Log.d(TAG,"Processing direction vector");

                    //check if the vanishing point and the next vanishing point aren't too far from each other
                    if ((Math.pow((DxAve - Dxold), 2) + Math.pow((DyAve - Dyold), 2) < (150 * 150)) == true)  //distance 150 px max
                        Imgproc.line(rgbaImage, new Point(W/2, H/2), new Point(W / 2 + DxAve, H / 2 + DyAve), new Scalar(255, 0, 0), 7);

                    //walking directions
                    if ((Math.abs(DxAve) < 80) && (DyAve > 100) && Math.abs(Dxold - DxAve) < 20) {
                        state = "Straight";
                        Imgproc.putText(rgbaImage, state, new Point(50, 50), Core.FONT_HERSHEY_PLAIN, 3, new Scalar(255, 0, 0),
                                2, Imgproc.LINE_AA, false);
                    } else if ((DxAve > 80) && (DyAve > 100) && (Math.abs(Dxold - DxAve) < 20)) {
                        state = "Right";
                        Imgproc.putText(rgbaImage, state, new Point(50, 50), Core.FONT_HERSHEY_PLAIN, 3, new Scalar(255, 0, 0),
                                2, Imgproc.LINE_AA, false);
                    } else if ((DxAve < 80) && (DyAve > 100) && (Math.abs(Dxold - DxAve) < 20)) {
                        state = "Left";
                        Imgproc.putText(rgbaImage, state, new Point(50, 50), Core.FONT_HERSHEY_PLAIN, 3, new Scalar(255, 0, 0),
                                2, Imgproc.LINE_AA, false);
                    } else {
                        Imgproc.line(rgbaImage, new Point(W / 2, H / 2), new Point(W/2 + Dxold, H / 2 +
                                Dyold), new Scalar(0, 0, 255));
                    }

                    //walking directions
                    if (state == "Straight") {
                        Imgproc.putText(rgbaImage, state, new Point(50, 50), Core.FONT_HERSHEY_PLAIN, 3, new Scalar(0, 0, 0), 2, Imgproc.LINE_AA, false);
                    } else {
                        Imgproc.putText(rgbaImage, state, new Point(50, 50), Core.FONT_HERSHEY_PLAIN, 3, new Scalar(255, 0, 0),
                                2, Imgproc.LINE_AA, false);
                    }
                    Dxold = DxAve;
                    Dyold = DyAve;

                }
            }

        }
    }
}