import org.opencv.core.Scalar;

public class ColorRange {
    public Scalar lowerBound, upperBound;

    public ColorRange(Scalar lowerBound, Scalar upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    @Override
    public String toString() {
        return lowerBound.toString() + " " + upperBound.toString();
    }
}
