package org.opencv.samples.seek;


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
    double thresh = 150; //optimal 150
    int bw_width = 300; //#170

    //Cache
    //Mat rgbaImage = new Mat(); //image to process
    Mat mGray = new Mat(); //grey Scale
    Mat mThresh1 = new Mat();
    Mat mEdges = new Mat();
    Mat mHierarchy = new Mat();
    Scalar lineColor = new Scalar(0, 255, 0);
    Scalar tealCircle = new Scalar(0, 255, 255);

    private java.util.List<MatOfPoint> mContours = new ArrayList<>();
    java.util.List<MatOfPoint> listwrapper;

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

        Iterator<MatOfPoint> each = contours.iterator();

        while(each.hasNext()){
            MatOfPoint wrapper = each.next();
            Rect rect = Imgproc.boundingRect(wrapper);

            if (rect.width > bw_width){
//                (rect.y + rect.height)), lineColor);
//                bxRight.add(rect.x + rect.width);
//                byRight.add(rect.y);
//                bxLeft.add(rect.x);
//                byLeft.add(rect.y);
//                bxbyLeftArray.add(new Point(rect.x, rect.y));
//                bxbyRightArray.add(new Point((rect.x + rect.width), rect.y));
                Imgproc.line(rgbaImage, new Point(rect.x, rect.y), new Point((rect.x + rect.width), rect.y), lineColor,2);
                Imgproc.circle(rgbaImage, new Point(rect.x, rect.y), 10, tealCircle, 2);
                Imgproc.circle(rgbaImage, new Point((rect.x + rect.width), rect.y), 10, tealCircle, 2);
            }
        }
    }
}
