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
    // Note: HSV hue [0, 179] (not [0, 359]).
    private static Mat blurred = new Mat();
    private static Mat hsv = new Mat();
    private static Mat mask1 = new Mat(), mask2 = new Mat();
    private static Mat hierarchy = new Mat();
    private static final Scalar GREEN_COLOR = new Scalar(0, 255, 0);
    private static final Scalar RED_COLOR = new Scalar(0, 0, 255);
    private static final Scalar BLUE_COLOR = new Scalar(255, 0, 0);
    private static final Scalar BLACK_COLOR = new Scalar(0, 0, 0);
    private static final Scalar WHITE_COLOR = new Scalar(255, 255, 255);
    private static final Scalar GREY_COLOR = new Scalar(100, 100, 100);

    public static void main(String[] args) {

        Scalar lowerRed = new Scalar(0, 95, 95);
        Scalar upperRed = new Scalar(10, 255, 255);
        Scalar lowerBlue = new Scalar(100, 80, 80);
        Scalar upperBlue = new Scalar(130, 255, 255);

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
            List<VisionObject> objs = coloredObjectCoordinates(input, 0.01, 0.0, test.ranges);
            // src, gaussian kernel size
            Mat g = gaussian(input).clone();
            // src, ranges
            Mat m = mask(hsv, test.ranges).clone();

            Mat temp = new Mat();
            Imgproc.cvtColor(g, temp, Imgproc.COLOR_BGR2HSV);
            Mat gm = mask(temp, test.ranges).clone();

            // src
            List<MatOfPoint> contours = contours(gm);

            highlightContours(input, contours, RED_COLOR);

            System.out.println(String.format("Test: %s\n\t%s", test.path, test.ranges[0]));
            for (VisionObject obj : objs) {
                highlightObject(input, obj, GREEN_COLOR);

                String text = String.format("(%d, %d, %f, %f)", obj.x, obj.y, obj.pixelTotality, obj.boundingTotality);
                Size textSize = Imgproc.getTextSize(text, Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, 1, null);
                int x = (int)Math.min(Math.max(textSize.width / 2, obj.x), input.width() - textSize.width / 2);
                int y = (int)Math.min(Math.max(textSize.height / 2, obj.y), input.height() - textSize.height / 2);
                simpleText(input, text, x, y, true, 0.5, WHITE_COLOR, 2);
                simpleText(input, text, x, y, true, 0.5, BLACK_COLOR, 1);

                System.out.println(String.format("\n\tObject: %s", obj));
            }
            output(outputDirectory + test.path.replace(directory, ""), 1, input);
            output(outputDirectory + test.path.replace(directory, ""), 2, g);
            output(outputDirectory + test.path.replace(directory, ""), 3, m);
            output(outputDirectory + test.path.replace(directory, ""), 4, gm);
        }
    }

    public static List<VisionObject> coloredObjectCoordinates(
        Mat src_dest, 
        double minimumPixelTotality, 
        double minimumBoundingTotality, 
        // boolean draw, 
        List<ColorRange> ranges_hsv
    ) {
        return coloredObjectCoordinates(src_dest, minimumPixelTotality, minimumBoundingTotality, /*draw, */ranges_hsv.toArray(new ColorRange[0]));
    }
    public static List<VisionObject> coloredObjectCoordinates(
        Mat src_dest, 
        double minimumPixelTotality, 
        double minimumBoundingTotality, 
        // boolean draw, 
        ColorRange ...ranges_hsv
    ) {
        return coloredObjectCoordinates(src_dest, minimumPixelTotality, minimumBoundingTotality, new Size(5, 5), /*draw, */ranges_hsv);
    }
    public static List<VisionObject> coloredObjectCoordinates(
        Mat src_dest, 
        double minimumPixelTotality, 
        double minimumBoundingTotality, 
        Size gaussianKernelSize, 
        // boolean draw, 
        ColorRange ...ranges_hsv
    ) {
        gaussian(src_dest, gaussianKernelSize);
        Imgproc.cvtColor(blurred, hsv, Imgproc.COLOR_BGR2HSV);
        mask(hsv, ranges_hsv);

        List<MatOfPoint> contours = contours(mask1);

        List<VisionObject> objs = new ArrayList<>();
        double total = mask1.cols() * mask1.rows();
        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);

            double pixelTotality = Core.countNonZero(mask1.submat(rect)) / total;
            double boundingTotality = rect.width * rect.height / total;
            if (
                pixelTotality >= minimumPixelTotality 
                && boundingTotality >= minimumBoundingTotality
            ) {
                objs.add(
                    new VisionObject(
                        rect.x + rect.width / 2, 
                        rect.y + rect.height / 2, 
                        rect.width, 
                        rect.height, 
                        pixelTotality, 
                        boundingTotality
                    )
                );
            }
        }

        // if (draw) {
        //     highlightContours(src_dest, contours, RED_COLOR);
        //     for (VisionObject obj : objs) {
        //         ObjectDetector.highlightObject(src_dest, obj, GREEN_COLOR);
        //     }
        // }

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
        int x = obj.x - obj.width / 2;
        int y = obj.y - obj.height / 2;
        rectangle(
            src_dest, 
            x, 
            y, 
            x + obj.width, 
            y + obj.height, 
            color, 
            2
        );
    }

    public static void highlightContours(Mat src_dest, List<MatOfPoint> contours, Scalar color) {
        Imgproc.drawContours(src_dest, contours, -1, color, 2);
    }

    public static void rectangle(Mat src_dest, int x1, int y1, int x2, int y2, Scalar color, int thickness) {
        Point p1 = new Point(x1, y1);
        Point p2 = new Point(x2, y2);
        Imgproc.rectangle(
            src_dest,
            p1, 
            p2, 
            color,
            2
        );
    }

    public static void simpleText(
        Mat src_dest, 
        String text, 
        int x, 
        int y, 
        boolean center, 
        double scale, 
        Scalar color, 
        int thickness
    ) {
        if (center) {
            Size textSize = Imgproc.getTextSize(text, Imgproc.FONT_HERSHEY_SIMPLEX, scale, 1, null);
            x -= textSize.width / 2;
            y += textSize.height / 2;
        }
        Imgproc.putText(src_dest, text, new Point(x, y), Imgproc.FONT_HERSHEY_SIMPLEX, scale, color, thickness);
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
