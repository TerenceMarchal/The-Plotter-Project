package common.engine;

import java.util.Locale;

import image_processing.enums.OutputMirroring;
import streaming.enums.PlotterSetting;
import streaming.session.PlotterConfiguration;

/**
 * A class representing a line stroke
 * 
 * @author Terence
 *
 */
public class Line implements Cloneable {

    /**
     * The line coordinates, in mm or px depending on the context
     */
    public final double x0, y0, x1, y1;

    /**
     * Instantiate a new line
     * 
     * @param x0 the start X coordinate
     * @param y0 the start Y coordinate
     * @param x1 the end X coordinate
     * @param y1 the end Y coordinate
     */
    public Line(double x0, double y0, double x1, double y1) {
        this(x0, y0, x1, y1, false);
    }

    /**
     * Instantiate a new line
     * 
     * @param x0              the start X coordinate
     * @param y0              the start Y coordinate
     * @param x1              the end X coordinate
     * @param y1              the end Y coordinate
     * @param invertDirection true to invert the start and end coordinates
     */
    public Line(double x0, double y0, double x1, double y1, boolean invertDirection) {
        if (!invertDirection) {
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
        } else {
            this.x0 = x1;
            this.y0 = y1;
            this.x1 = x0;
            this.y1 = y0;
        }
    }

    /**
     * Compute the squared flying distance to an other line
     * 
     * @param other the other line from which to compute the squared flying distance
     * @return the squared flying distance to an other line
     */
    public double getSquaredFlyingDistanceTo(Line other) {
        return Math.pow(other.x0 - x1, 2) + Math.pow(other.y0 - y1, 2);
    }

    /**
     * Compute the cost from an other line
     * 
     * @param other the other line from which to compute the cost
     * @return the cost from an other line
     */
    public double getCostFrom(Line other) {
        if (Path.USE_FLYING_DURATION_AS_COST) {
            return Utils.computeXYTravelDuration(x0, y0, other.x1, other.y1,
                    PlotterConfiguration.Instance.getDoubleSettingValue(PlotterSetting.XY_ACCELERATION),
                    PlotterConfiguration.Instance.getDoubleSettingValue(PlotterSetting.FLYING_XY_MAX_SPEED),
                    PlotterConfiguration.Instance.getBooleanSettingValue(PlotterSetting.IS_CORE_XY));
        } else {
            return getSquaredFlyingDistanceTo(other);
        }
    }

    /**
     * Compute the line squared length
     * 
     * @return the line squared length
     */
    public double getSquaredLength() {
        return Math.pow(x1 - x0, 2) + Math.pow(y1 - y0, 2);
    }

    /**
     * Compute the flying distance to the specified line
     * 
     * @param other the other line to which to compute the flying distance
     * @return the flying distance to the specified line
     */
    public double getFlyingDistanceTo(Line other) {
        return Math.sqrt(getSquaredFlyingDistanceTo(other));
    }

    /**
     * Indicate if this line is aligned with the specified line
     * 
     * @param other the other line for which to check the alignment
     * @return true if this line is aligned with the specified line, false otherwise
     */
    public boolean isAlignedWith(Line other) {
        return (other.y0 - y1) * (x0 - other.x0) - (y0 - other.y0) * (other.x0 - x1) == 0
                && (other.y1 - y1) * (x0 - other.x1) - (y0 - other.y1) * (other.x1 - x1) == 0;
    }

    /**
     * Note: we assume that there will never be two drawed lines with the same starting and ending points, no matter the
     * direction
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Line) {
            Line l = (Line) o;
            return x0 == l.x0 && y0 == l.y0 && x1 == l.x1 && y1 == l.y1;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + new Double(x0).hashCode();
        hash = 31 * hash + new Double(y0).hashCode();
        hash = 31 * hash + new Double(x1).hashCode();
        hash = 31 * hash + new Double(y1).hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "[Line (%f;%f) -> (%f;%f)]", x0, y0, x1, y1);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return new Line(x0, y0, x1, y1);
    }

    /**
     * Create a new line from the specified one, inverting its direction
     * 
     * @param line the line from which to invert the direction
     * @return a new line with the inverted direction
     */
    public static Line createInvertedLine(Line line) {
        return new Line(line.x1, line.y1, line.x0, line.y0);
    }

    /**
     * Create a new line from the specified one, rotated from the specified angle around the specified position
     * 
     * @param line  the line from which to rotate the new line
     * @param angle the angle in degrees to which to rotate the new line
     * @param rX    the X position around which to rotate the new line
     * @param rY    the Y position around which to rotate the new line
     * @return a new line, rotated from the specified angle around the specified position
     */
    public static Line createRotatedLine(Line line, int angle, double rX, double rY) {
        double a = Math.toRadians(angle);
        double X0 = rX + (line.x0 - rX) * Math.cos(a) - (line.y0 - rY) * Math.sin(a);
        double Y0 = rY + (line.x0 - rX) * Math.sin(a) + (line.y0 - rY) * Math.cos(a);
        double X1 = rX + (line.x1 - rX) * Math.cos(a) - (line.y1 - rY) * Math.sin(a);
        double Y1 = rY + (line.x1 - rX) * Math.sin(a) + (line.y1 - rY) * Math.cos(a);
        return new Line(X0, Y0, X1, Y1);
    }

    /**
     * Create a new line from the specified one, translated by the specified offsets
     * 
     * @param line the line from which to translate the new line
     * @param trX  the X offset to which translate the new line
     * @param trY  the Y offset to which translate the new line
     * @return a new line, translated by the specified offsets
     */
    public static Line createTranslatedLine(Line line, double trX, double trY) {
        return new Line(line.x0 + trX, line.y0 + trY, line.x1 + trX, line.y1 + trY);
    }

    /**
     * Create a new line from the specified one, mirrored according to the specified OutputMirroring
     * 
     * TODO-029: it would probably be better to specify the X and Y coordinates of the mirroring instead of xMax and
     * yMax
     * 
     * @param line      the line from which to mirror the new one
     * @param mirroring the OutputMirroring from which to retrieves to mirroring axis
     * @param xMax      the X coordinate after mirroring of a point at x=0 for an X-axis mirroring
     * @param yMax      the Y coordinate after mirroring of a point at y=0 for an Y-axis mirroring
     * @return a new line, mirrored according to the specified OutputMirroring
     */
    public static Line createMirroredLine(Line line, OutputMirroring mirroring, double xMax, double yMax) {
        double X0 = line.x0;
        double Y0 = line.y0;
        double X1 = line.x1;
        double Y1 = line.y1;
        if (mirroring.xMirroring()) {
            X0 = xMax - X0;
            X1 = xMax - X1;
        }
        if (mirroring.yMirroring()) {
            Y0 = yMax - Y0;
            Y1 = yMax - Y1;
        }
        return new Line(X0, Y0, X1, Y1);
    }

}
