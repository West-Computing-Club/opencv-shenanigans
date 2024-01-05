import org.opencv.core.Point;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class Main {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) {
        // Load the image
        // 316 x 421
        Mat src = Imgcodecs.imread("resources/red1.webp");

        // Define the two rectangles
        Point p1 = new Point (0, 0), p2 = new Point (106, 421);
        Point p3 = new Point (106, 0), p4 = new Point(212, 421);    
        Rect rect1 = new Rect(p1, p2); // Set these values
        Rect rect2 = new Rect(p3, p4); // Set these values

        // Convert to HSV
        Mat hsv = new Mat();
        Imgproc.cvtColor(src, hsv, Imgproc.COLOR_BGR2HSV);

        // Define the color range for the cube
        // Hue goes to 179, all others go to 255.
        Scalar[] redRanges = {new Scalar (11, 0.40, 0.29), new Scalar (11, 0.50, 0.96)};
        Scalar[] blueRanges = {new Scalar (232, 0.34, 0.22), new Scalar (232, 0.54, 0.87)};
        Scalar lowerRed1 = new Scalar(0, 100, 100);
        Scalar upperRed1 = new Scalar(10, 255, 255);
        Scalar lowerBlue = new Scalar(100, 150, 50);
        Scalar upperBlue = new Scalar(140, 255, 255);
        Scalar lowerColor = lowerRed1; // Set these values
        Scalar upperColor = upperRed1; // Set these values

        // Create a mask
        Mat mask = new Mat();
        Core.inRange(hsv, lowerColor, upperColor, mask);

        // Check if the cube is in the rectangles
        int count1 = Core.countNonZero(mask.submat(rect1));
        int count2 = Core.countNonZero(mask.submat(rect2));

        // Output result
        System.out.println(count1 + " " + count2);
    }
}
