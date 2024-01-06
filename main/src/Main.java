import org.opencv.imgcodecs.Imgcodecs;

import org.opencv.imgproc.Imgproc;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Point;
import org.opencv.core.Core;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

public class Main {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    // Static auxilary matrices since EOCV won't automatically release matrices per frame process.
    private static Mat blurred = new Mat();
    private static Mat hsv = new Mat();
    private static Mat mask1 = new Mat(), mask2 = new Mat();
    private static Mat hierarchy = new Mat();
    private static final Scalar GREEN_COLOR = new Scalar(0, 255, 0);
    private static final Scalar RED_COLOR = new Scalar(0, 0, 255);

    public static void main(String[] args) {

        Scalar lowerRed = new Scalar(0, 100, 100);
        Scalar upperRed = new Scalar(10, 255, 255);
        Scalar lowerBlue = new Scalar(100, 100, 100);
        Scalar upperBlue = new Scalar(225, 255, 255);

        String directory = "resources/";
        String outputDirectory = "output/";

        Test[] tests = {
            new Test(directory + "blue1.webp", new ColorRange[] {new ColorRange(lowerBlue, upperBlue)}), 
            new Test(directory + "blue2.webp", new ColorRange[] {new ColorRange(lowerBlue, upperBlue)}), 
            new Test(directory + "blue3.webp", new ColorRange[] {new ColorRange(lowerBlue, upperBlue)}), 
            new Test(directory + "red1.webp", new ColorRange[] {new ColorRange(lowerRed, upperRed)}), 
            new Test(directory + "red2.webp", new ColorRange[] {new ColorRange(lowerRed, upperRed)})
        };

        for (Test test : tests) {
            Mat input = Imgcodecs.imread(test.path);

            // src, threshold, gaussian kernel size, HSV ranges
            List<VisionObject> objs = coloredObjectCoordinates(input, 0.01, test.ranges);
            // src, gaussian kernel size
            Mat g = gaussian(input).clone();
            // src, ranges
            Mat m = mask(input, test.ranges).clone();

            Mat gm = mask(g, test.ranges).clone();

            // src
            List<MatOfPoint> contours = contours(gm);

            System.out.println(String.format("Test: %s\n\t%s", test.path, test.ranges[0]));
            for (VisionObject obj : objs) {
                highlightObject(input, obj, GREEN_COLOR);

                System.out.println(String.format("\n\tObject: %s", obj));
            }
            highlightContours(input, contours, RED_COLOR);
            output(outputDirectory + test.path.replace(directory, ""), 1, input);
            // output(outputDirectory + test.path.replace(directory, ""), 2, g);
            // output(outputDirectory + test.path.replace(directory, ""), 3, m);
            // output(outputDirectory + test.path.replace(directory, ""), 4, gm);
        }
    }

    public static List<VisionObject> coloredObjectCoordinates(Mat src, double minimumTotality, List<ColorRange> ranges_hsv) {
        return coloredObjectCoordinates(src, minimumTotality, ranges_hsv.toArray(new ColorRange[0]));
    }
    public static List<VisionObject> coloredObjectCoordinates(Mat src, double minimumTotality, ColorRange ...ranges_hsv) {
        return coloredObjectCoordinates(src, minimumTotality, new Size(5, 5), ranges_hsv);
    }
    public static List<VisionObject> coloredObjectCoordinates(Mat src, double minimumTotality, Size gaussianKernelSize, ColorRange ...ranges_hsv) {
        gaussian(src, gaussianKernelSize);
        Imgproc.cvtColor(blurred, hsv, Imgproc.COLOR_BGR2HSV);
        mask(hsv, ranges_hsv);

        List<MatOfPoint> contours = contours(mask1);

        List<VisionObject> objs = new ArrayList<>();
        double total = mask1.cols() * mask1.rows();
        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);

            double totality = rect.width * rect.height / total;
            if (totality >= minimumTotality) {
                objs.add(
                    new VisionObject(
                        rect.x + rect.width / 2, 
                        rect.y + rect.height / 2, 
                        rect.width, 
                        rect.height, 
                        totality
                    )
                );
            }
        }
        return objs;
    }

    // Applying a Gaussian blur to the image prior to contour detection can reduce noise.
    public static Mat gaussian(Mat src) {
        return gaussian(src, new Size(5, 5));
    }
    public static Mat gaussian(Mat src, Size gaussianKernelSize) {
        Imgproc.GaussianBlur(src, blurred, gaussianKernelSize, 0);
        return blurred;
    }

    public static Mat mask(Mat src, List<ColorRange> ranges) {
        return mask(src, ranges.toArray(new ColorRange[0]));
    }
    public static Mat mask(Mat src, ColorRange ...ranges) {
        mask1.setTo(new Scalar(0, 0, 0, 0));
        mask2.setTo(new Scalar(0, 0, 0, 0));
        for (ColorRange range : ranges) {
            Core.inRange(src, range.lowerBound, range.upperBound, mask2);

            if (mask1.cols() != mask2.cols() || mask1.rows() != mask2.rows()) {
                mask2.copyTo(mask1);
            }
            Core.bitwise_or(mask1, mask2, mask1);
        }
        return mask1;
    }

    public static List<MatOfPoint> contours(Mat src) {
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(src, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        return contours;
    }

    public static void highlightObject(Mat src_dest, VisionObject obj, Scalar color) {
        Point p1 = new Point(obj.x - obj.width / 2, obj.y - obj.height / 2);
        Point p2 = new Point(p1.x + obj.width, p1.y + obj.height);
        Imgproc.rectangle(
            src_dest,
            p1, 
            p2, 
            color,
            2
        );
    }

    public static void highlightContours(Mat src_dest, List<MatOfPoint> contours, Scalar color) {
        Imgproc.drawContours(src_dest, contours, -1, color, 2);
    }

    public static void output(String name, int id, Mat input) {
        Imgcodecs.imwrite(name + "_" + id + ".png", input);
    }

    private static class Test {
        private String path;
        private ColorRange[] ranges;

        private Test(String path, ColorRange[] ranges) {
            this.path = path;
            this.ranges = ranges;
        }
    }
}
