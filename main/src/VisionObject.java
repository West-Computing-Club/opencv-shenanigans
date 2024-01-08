public class VisionObject {

    // Units are in pixels.
    // The origin (0, 0) is located in the upper left most corner.
    public int x, y, width, height;
    // Totality is a ratio of the number of pixels assumed to be a part of the object to the total number of pixels.
    public double pixelTotality, boundingTotality;

    public VisionObject(int x, int y, int width, int height, double pixelTotality, double boundingTotality) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.pixelTotality = pixelTotality;
        this.boundingTotality = boundingTotality;
    }

    @Override
    public String toString() {
        return String.format(
            "x = %d, y = %d, width = %d, height = %d, pixel totality = %.3f, bounding totality = %.3f", 
            x, y, width, height, pixelTotality, boundingTotality
        );
    }
}
