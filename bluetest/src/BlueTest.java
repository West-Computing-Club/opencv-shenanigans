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


class BlueTest {

  private static final Scalar GREEN_COLOR = new Scalar(0, 255, 0);
  private static final Scalar RED_COLOR = new Scalar(0, 0, 255);
  private static final Scalar BLUE_COLOR = new Scalar(255, 0, 0);
  private static final Scalar BLACK_COLOR = new Scalar(0, 0, 0);
  private static final Scalar WHITE_COLOR = new Scalar(255, 255, 255);
  private static final Scalar GREY_COLOR = new Scalar(100, 100, 100);
  static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }
  
  public static void main(String[] args) {
    
    Scalar lowerBlue = new Scalar(100, 20, 20);
    Scalar upperBlue = new Scalar(130, 255, 255);

    double minimumPixelTotality = .002;
    double minimumBoundingTotality = 0;

    Mat input = Imgcodecs.imread("person.jpg");
    Mat blurred = new Mat();
    Mat hsv = new Mat();
    Mat mask1 = new Mat();
    Mat hierarchy = new Mat();

    Imgproc.GaussianBlur(input, blurred, new Size(5,5), 0);
    Imgproc.cvtColor(blurred, hsv, Imgproc.COLOR_BGR2HSV);
    Core.inRange(hsv, lowerBlue, upperBlue, mask1);

    List<MatOfPoint> contours = new ArrayList<>();
    Imgproc.findContours(mask1, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

    
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
  
    for (VisionObject obj : objs) {
      highlightObject(input, obj, GREEN_COLOR);

      String text = String.format("(%d, %d, %f, %f)", obj.x, obj.y, obj.pixelTotality, obj.boundingTotality);
      Size textSize = Imgproc.getTextSize(text, Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, 1, null);
      int x = (int)Math.min(Math.max(textSize.width / 2, obj.x), input.width() - textSize.width / 2);
      int y = (int)Math.min(Math.max(textSize.height / 2, obj.y), input.height() - textSize.height / 2);
     // simpleText(input, text, x, y, true, 3, WHITE_COLOR, 2);
     // simpleText(input, text, x, y, true, 5, BLACK_COLOR, 1);

      System.out.println(String.format("\n\tObject: %s", obj));
  }

    output("output.jpg", 0, input);
    output("output1.jpg", 0, mask1);

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
        10
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
        thickness
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




}
