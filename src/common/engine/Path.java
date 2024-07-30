package common.engine;

import java.util.ArrayList;
import java.util.Collections;

import streaming.enums.PlotterSetting;
import streaming.session.PlotterConfiguration;

/**
 * A class representing a Path, i.e. a set of lines to draw
 * 
 * @author Terence
 *
 */
public class Path implements Cloneable {

    /**
     * Indicate if the flying (i.e. the non-drawing motion) duration should be used to estimate a Path cost, as opposed
     * to the flying legth (i.e. distance)
     * 
     */
    public static final boolean USE_FLYING_DURATION_AS_COST = true;

    /**
     * The lines to draw of the Path
     */
    private ArrayList<Line> lines;

    /**
     * The Z height at which the clear (i.e. flying) motions are executed
     */
    private final double zClearHeight;

    /**
     * The Path flying distance, i.e. the sum of the distances between the lines to draw
     */
    private double flyingDistance;

    /**
     * The Path squared flying distance
     */
    private double squaredFlyingDistance;

    /**
     * The Path flying duration, in s
     */
    private double flyingDuration;

    /**
     * The Path drawing distance
     */
    private double drawingDistance;

    /**
     * The Path drawing duration, in s
     */
    private double drawingDuration;

    /**
     * The distance of the up and down motions along the Z-axis
     */
    private double upDownDistance;

    /**
     * The duration of the up and down motions along the Z-axis, in s
     */
    private double upDownDuration;

    /**
     * A flag indicating if the Path distances and durations should be recalculated
     */
    private boolean shouldRecomputeDistancesAndDurations = true;

    /**
     * Instantiate a new Path with the specified lines to draw
     * 
     * @param lines        the lines to draw
     * @param zClearHeight the height at which the flying motions should be executed
     */
    public Path(ArrayList<Line> lines, double zClearHeight) {
        this.lines = lines;
        this.zClearHeight = zClearHeight;
    }

    /**
     * Instantiate a new empty Path
     * 
     * @param zClearHeight the height at which the flying motions should be executed
     */
    public Path(double zClearHeight) {
        this(new ArrayList<Line>(), zClearHeight);
    }

    /**
     * Get the height at which the flying motions should be executed
     * 
     * @return the height at which the flying motions should be executed
     */
    public double getZClearHeight() {
        return zClearHeight;
    }

    /**
     * Retrieve the lines to draw of the Path
     * 
     * @return the lines to draw of the Path
     */
    public ArrayList<Line> getLines() {
        return lines;
    }

    /**
     * Add a line to draw to the Path
     * 
     * @param line the line to add to the Path
     */
    public void addLine(Line line) {
        lines.add(line);
        shouldRecomputeDistancesAndDurations = true;
    }

    /**
     * Remove the line at the specified index from the Path
     * 
     * @param id the index of the line to remove from the Path
     */
    public void removeLine(int id) {
        lines.remove(id);
        shouldRecomputeDistancesAndDurations = true;
    }

    /**
     * Get the line at the specified index of the Path
     * 
     * @param id the ID of the line to retrieve
     * @return the line at the specified index of the Path
     */
    public Line getLine(int id) {
        return lines.get(id);
    }

    /**
     * Replace the line at the specified index by an other one
     * 
     * @param id   the ID of the line to replace
     * @param line the new line with which to replace the old one
     */
    public void setLine(int id, Line line) {
        lines.set(id, line);
        shouldRecomputeDistancesAndDurations = true;
    }

    /**
     * Get the number of lines to draw of the Path
     * 
     * @return the number of lines to draw of the Path
     */
    public int getNbLines() {
        return lines.size();
    }

    /**
     * Get the index in the Path of the specified line, without taking into account its direction
     * 
     * @param line the line for which to retrieve the index in the Path
     * @return the index of the line in the Path, or -1 if it is not present in the Path
     */
    public int getIndexInPath(Line line) {
        int id = lines.indexOf(line);
        if (id == -1) {
            id = lines.indexOf(Line.createInvertedLine(line));
        } else {
            int idInverted = lines.indexOf(Line.createInvertedLine(line));
            if (idInverted != -1 && idInverted < id) {
                id = idInverted;
            }
        }
        return id;
    }

    /**
     * Recalculate the flying and drawing distances and durations of the Path
     */
    private void recomputeDistancesAndDurations() {
        // TODO-024: optimize that, use a single loop
        squaredFlyingDistance = 0;
        flyingDistance = 0;
        for (int i = 1; i < lines.size(); i++) {
            Line line0 = lines.get(i - 1);
            Line line1 = lines.get(i);
            squaredFlyingDistance += line1.getCostFrom(line0);
            flyingDistance += line1.getFlyingDistanceTo(line0);
        }

        drawingDistance = 0;
        for (Line line : lines) {
            drawingDistance += Math.sqrt(Math.pow(line.x1 - line.x0, 2) + Math.pow(line.y1 - line.y0, 2));
        }

        upDownDistance = 2 * lines.size() * zClearHeight;
        double zClearDuration = Utils.computeZTravelDuration(0, zClearHeight,
                PlotterConfiguration.Instance.getDoubleSettingValue(PlotterSetting.Z_ACCELERATION),
                PlotterConfiguration.Instance.getDoubleSettingValue(PlotterSetting.Z_MAX_SPEED),
                PlotterConfiguration.Instance.getBooleanSettingValue(PlotterSetting.IS_CORE_XY));
        upDownDuration = zClearDuration * 2;
        drawingDuration = 0;
        flyingDuration = 0;
        double lastX = Double.MAX_VALUE;
        double lastY = Double.MAX_VALUE;
        for (int idLine = 0; idLine < lines.size(); idLine++) {
            Line line = lines.get(idLine);
            if (idLine > 0 && (line.x0 != lastX || line.y0 != lastY)) {
                flyingDuration += Utils.computeXYTravelDuration(lastX, lastY, line.x0, line.y0,
                        PlotterConfiguration.Instance.getDoubleSettingValue(PlotterSetting.XY_ACCELERATION),
                        PlotterConfiguration.Instance.getDoubleSettingValue(PlotterSetting.FLYING_XY_MAX_SPEED),
                        PlotterConfiguration.Instance.getBooleanSettingValue(PlotterSetting.IS_CORE_XY));
                upDownDuration += zClearDuration * 2;
            }
            drawingDuration += Utils.computeXYTravelDuration(line.x0, line.y0, line.x1, line.y1,
                    PlotterConfiguration.Instance.getDoubleSettingValue(PlotterSetting.XY_ACCELERATION),
                    PlotterConfiguration.Instance.getDoubleSettingValue(PlotterSetting.DRAWING_XY_MAX_SPEED),
                    PlotterConfiguration.Instance.getBooleanSettingValue(PlotterSetting.IS_CORE_XY));
            lastX = line.x1;
            lastY = line.y1;
        }

        shouldRecomputeDistancesAndDurations = false;
    }

    /**
     * Get the Path squared flying distance
     * 
     * @return the Path squared flying distance
     */
    private double getSquaredFlyingDistance() {
        if (shouldRecomputeDistancesAndDurations) {
            recomputeDistancesAndDurations();
        }
        return squaredFlyingDistance;
    }

    /**
     * Get the Path cost
     * 
     * @return the Path cost
     */
    public double getCost() {
        if (USE_FLYING_DURATION_AS_COST) {
            return getFlyingDuration();
        } else {
            return getSquaredFlyingDistance();
        }

    }

    /**
     * Get the Path fitness, i.e. the invert of its cost
     * 
     * @return the Path fitness
     */
    public double getFitness() {
        return 1 / getCost();
    }

    /**
     * Get the Path flying distance
     * 
     * @return the Path flying distance
     */
    public double getFlyingDistance() {
        if (shouldRecomputeDistancesAndDurations) {
            recomputeDistancesAndDurations();
        }
        return flyingDistance;
    }

    /**
     * Get the Path flying and up-down motions distance
     * 
     * @return the Path flying and up-down motions distance
     */
    public double getFlyingAndUpDownDistance() {
        if (shouldRecomputeDistancesAndDurations) {
            recomputeDistancesAndDurations();
        }
        return flyingDistance + upDownDistance;
    }

    /**
     * Get the Path up-down motions distance
     * 
     * @return the Path up-down motions distance
     */
    public double getUpDownDistance() {
        if (shouldRecomputeDistancesAndDurations) {
            recomputeDistancesAndDurations();
        }
        return upDownDistance;
    }

    /**
     * Get the Path drawing distance
     * 
     * @return the Path drawing distance
     */
    public double getDrawingDistance() {
        if (shouldRecomputeDistancesAndDurations) {
            recomputeDistancesAndDurations();
        }
        return drawingDistance;
    }

    /**
     * Get the Path drawing duration, in s
     * 
     * @return the Path drawing duration
     */
    public double getDrawingDuration() {
        if (shouldRecomputeDistancesAndDurations) {
            recomputeDistancesAndDurations();
        }
        return drawingDuration;
    }

    /**
     * Get the Path flying duration, in s
     * 
     * @return the Path flying duration
     */
    public double getFlyingDuration() {
        if (shouldRecomputeDistancesAndDurations) {
            recomputeDistancesAndDurations();
        }
        return flyingDuration;
    }

    /**
     * Get the Path flying and up-down motions duration, in s
     * 
     * @return the Path flying and up-down motions duration
     */
    public double getFlyingAndUpDownDuration() {
        if (shouldRecomputeDistancesAndDurations) {
            recomputeDistancesAndDurations();
        }
        return flyingDuration + upDownDuration;
    }

    /**
     * Get the Path total duration, in s
     * 
     * @return the Path total duration, in s
     */
    public double getTotalDuration() {
        if (shouldRecomputeDistancesAndDurations) {
            recomputeDistancesAndDurations();
        }
        return drawingDuration + flyingDuration + upDownDuration;
    }

    /**
     * Convert the Path into a Path with units in pixels instead of mm
     * 
     * @param dpi the DPI to use
     * @return a copy of the Path with its units in pixels instead of mm
     */
    public Path convertFromPxToMm(int dpi) {
        Path convertedPath = new Path(zClearHeight);
        for (Line line : lines) {
            convertedPath.addLine(new Line(Utils.pxToMm((int) line.x0, dpi), Utils.pxToMm((int) line.y0, dpi),
                    Utils.pxToMm((int) line.x1, dpi), Utils.pxToMm((int) line.y1, dpi)));
        }
        return convertedPath;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return new Path((ArrayList<Line>) lines.clone(), zClearHeight);
    }

    @Override
    public String toString() {
        String str = "[Path: ";
        for (Line line : lines) {
            str += (line != null ? line.toString() : null) + " ";
        }
        return str + "]";
    }

    /**
     * Optimize a Path lines directions, to minimize its cost
     * 
     * @param path the Path to optimize
     * @return the optimized Path
     */
    public static Path optimizeLinesDirections(Path path) {
        // TODO-024: optimize that to reduce computation time
        Path optimizedPath = new Path(path.zClearHeight);
        Line lastLine = path.lines.get(0);
        optimizedPath.addLine(lastLine);
        for (int i = 1; i < path.lines.size(); i++) {
            Line line = path.lines.get(i);
            Line invertedLine = Line.createInvertedLine(line);
            double d0 = line.getCostFrom(lastLine);
            double d1 = invertedLine.getCostFrom(lastLine);
            if (d1 < d0) {
                optimizedPath.addLine(invertedLine);
                lastLine = invertedLine;
            } else {
                optimizedPath.addLine(line);
                lastLine = line;
            }
        }
        return optimizedPath;
    }

    /**
     * Get a copy of the specified Path, with the order of its line randomly shuffled
     * 
     * @param path the Path to copy and shuffle
     * @return a copy of the specified Path, with the order of its line randomly shuffled
     */
    public static Path getShuffledCopy(Path path) {
        ArrayList<Line> lines = (ArrayList<Line>) path.getLines().clone();
        Collections.shuffle(lines);
        return new Path(lines, path.getZClearHeight());
    }
}
