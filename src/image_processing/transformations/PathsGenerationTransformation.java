package image_processing.transformations;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

import common.engine.Ink;
import common.engine.Line;
import common.engine.Path;
import common.engine.SettingsSet;
import common.engine.Utils;
import image_processing.engine.Brush;
import image_processing.engine.BrushPalette;
import image_processing.engine.Image;
import image_processing.enums.Setting;
import image_processing.session.Project;

/**
 * A Transformation that generate the pens Paths
 * 
 * @author Terence
 *
 */
public class PathsGenerationTransformation extends AbstractTransformation {

    /**
     * The precision in mm used to transform an image into a path segment
     * 
     * TODO-039: use a setting
     */
    private static final double IMAGE_TO_PATH_PRECISION_IN_MM = 0.25;

    /**
     * An HashMap containing the paths per inks
     */
    private HashMap<Ink, Path> pathsPerInk = new HashMap<Ink, Path>();

    /**
     * Instantiate a Paths Generation Transformation
     */
    public PathsGenerationTransformation() {
        super(TransformationStep.PATHS_GENERATION,
                new Setting[] { Setting.LPMM_MAX, Setting.BRUSH_PALETTES, Setting.ID_SELECTED_BRUSH_PALETTE,
                        Setting.OUTLINE_LPMM, Setting.CLEAR_Z_HEIGHT, Setting.MIN_SEGMENT_LENGTH },
                true);
    }

    /**
     * Get the pens paths per ink
     * 
     * @return the pens paths per ink
     */
    public HashMap<Ink, Path> getPathsPerInk() {
        return pathsPerInk;
    }

    /**
     * Generate an optimized path from the specified path by reordering the segments to minimize the flying cost
     * 
     * @param path the path from which to generate the optimized one
     * @return the optimized path
     */
    private Path generateOptimizedPath(Path path) {
        if (path.getNbLines() == 0) {
            return path;
        }
        Path continuousPath = new Path(path.getZClearHeight());
        ArrayList<Line> remainingLines = (ArrayList<Line>) path.getLines().clone();
        Line lastLine = remainingLines.remove(0);
        continuousPath.addLine(lastLine);
        while (remainingLines.size() > 0) {
            int idClosestLine = 0;
            double smallestCost = Double.MAX_VALUE;
            for (int idLine = 0; idLine < remainingLines.size(); idLine++) {
                double cost = remainingLines.get(idLine).getCostFrom(lastLine);
                if (cost < smallestCost) {
                    idClosestLine = idLine;
                    smallestCost = cost;
                }
            }
            Line line = remainingLines.remove(idClosestLine);
            lastLine = line;
            continuousPath.addLine(line);
        }
        return continuousPath;
    }

    /**
     * Remove paths segments that are too short
     * 
     * Note: useful to reduce the drawing duration by limiting the up/down pen movements
     * 
     * TODO-048: rework and clean this function
     * 
     * @param pathsPerInk          the paths per ink
     * @param minSegmentLengthInMm the minimum length in mm a path segment must have to be kept
     * @return an HashMap containing the paths per ink cleaned from the paths segments too shord
     */
    private HashMap<Ink, Path> removeSegmentsTooShort(HashMap<Ink, Path> pathsPerInk, double minSegmentLengthInMm) {
        if (minSegmentLengthInMm == 0) {
            return pathsPerInk;
        }
        double minSquaredLength = minSegmentLengthInMm * minSegmentLengthInMm;
        HashMap<Ink, Path> cleanedPathsPerInk = new HashMap<Ink, Path>();
        for (Entry<Ink, Path> entry : pathsPerInk.entrySet()) {
//            Path path = entry.getValue();
//            Path cleanedPath = new Path(path.getZClearHeight());
//            boolean isPenUp = true;
//            for (int idLine = 0; idLine < path.getNbLines(); idLine++) {
//                Line line = path.getLine(idLine);
//                debugPath.addLine(new Line(line.x0, line.y0 + 10, line.x1, line.y1 + 10));
//                if (line.getSquaredLength() >= minSquaredLength) {
//                    cleanedPath.addLine(line);
//                }
//            }
//            Path cleanedPathBis = new Path(path.getZClearHeight());
//            for (int idLine = 0; idLine < cleanedPath.getNbLines(); idLine++) {
//                Line line = cleanedPath.getLine(idLine);
//                cleanedPathBis.addLine(line);
//                if (idLine < cleanedPath.getNbLines() - 1) {
//                    Line nextLine = cleanedPath.getLine(idLine + 1);
//
//                    if (line.getSquaredFlyingLengthTo(nextLine) < minSquaredLength) {
//                        if (line.isAlignedWith(nextLine)) {
//                            cleanedPathBis.addLine(new Line(line.x1, line.y1, nextLine.x0, nextLine.y0));
//                        }
//                    }
//                }
//            }
//            cleanedPathsPerInk.put(entry.getKey(), cleanedPathBis);

            // Path path = entry.getValue();
            Path path = generateOptimizedPath(entry.getValue());
            Path cleanedPath = new Path(path.getZClearHeight());
            for (int idLine = 0; idLine < path.getNbLines(); idLine++) {
                Line line = path.getLine(idLine);
                if (line.getSquaredLength() >= minSquaredLength) {
                    cleanedPath.addLine(line);
                } else {
                    Line previousLine = null;
                    double squaredDistToPreviousLine = Double.MAX_VALUE;
                    Line nextLine = null;
                    double squaredDistToNextLine = Double.MAX_VALUE;
                    if (idLine > 0) {
                        previousLine = path.getLine(idLine - 1);
                        squaredDistToPreviousLine = previousLine.getSquaredFlyingDistanceTo(line);
                    }
                    if (idLine < path.getNbLines() - 1) {
                        nextLine = path.getLine(idLine + 1);
                        squaredDistToNextLine = line.getSquaredFlyingDistanceTo(nextLine);
                    }
                    if (squaredDistToPreviousLine <= squaredDistToNextLine
                            && squaredDistToPreviousLine < minSquaredLength/* && line.isAlignedWith(previousLine) */) {
//                        cleanedPath.setLine(cleanedPath.getNbLines() - 1,
//                                new Line(previousLine.x0, previousLine.y0, line.x1, line.y1));
                        if (line.isAlignedWith(previousLine)) {
                            cleanedPath.addLine(new Line(previousLine.x1, previousLine.y1, line.x1, line.y1));
                        } else {
                            cleanedPath.addLine(new Line(previousLine.x1, previousLine.y1, line.x0, line.y0));
                            cleanedPath.addLine(new Line(line.x0, line.y0, line.x1, line.y1));
                        }
                    } else if (squaredDistToNextLine <= squaredDistToPreviousLine
                            && squaredDistToNextLine < minSquaredLength/* && line.isAlignedWith(nextLine) */) {
                        if (line.isAlignedWith(nextLine)) {
                            cleanedPath.addLine(new Line(line.x0, line.y0, nextLine.x0, nextLine.y0));
                        } else {
                            cleanedPath.addLine(new Line(line.x0, line.y0, line.x1, line.y1));
                            cleanedPath.addLine(new Line(line.x1, line.y1, nextLine.x0, nextLine.y0));
                        }
                    }
                }
            }
            cleanedPathsPerInk.put(entry.getKey(), cleanedPath);

//            for (int idLine = 0; idLine < path.getNbLines(); idLine++) {
//                Line line = path.getLine(idLine);
//                debugPath.addLine(new Line(line.x0, line.y0 + 10, line.x1, line.y1 + 10));
//                if (isPenUp) {
//                    if (line.getSquaredLength() >= minSquaredLength) {
//                        /* pen is up but the line is long enough, we draw it */
//                        cleanedPath.addLine(line);
//                        isPenUp = false;
//                    } else {
//                        /* pen is up and the line is too short, don't draw it */
//                        cleanedPath.addLine(line); // TODO-048: remove that
//                    }
//                } else {
//                    if (idLine < path.getNbLines() - 1) {
//                        Line nextLine = path.getLine(idLine + 1);
//                        double flyingDistance = line.getFlyingDistanceTo(nextLine);
//                        // TODO-048: check lines alignment
//                        if (flyingDistance * flyingDistance < minSquaredLength) {
//                            /* pen is down and the gap to the next line is short, merge this line with the next one */
////                            cleanedPath.addLine(new Line(line.x0, line.y0, nextLine.x1, nextLine.y1));
////                            idLine++;
////                            cleanedPath.addLine(line);
////                            cleanedPath.addLine(new Line(line.x1, line.y1, nextLine.x0, nextLine.y0));
//                            if (line.isAlignedWith(nextLine)) {
//                                cleanedPath.addLine(line);
//                                cleanedPath.addLine(new Line(line.x1, line.y1, nextLine.x0, nextLine.y0));
//                            } else {
//                                cleanedPath.addLine(line);
//                            }
//                        } else {
//                            /* pen down and gap to the next line long enough, draw the line and keep the gap */
//                            cleanedPath.addLine(line);
//                        }
//                    }
//                    isPenUp = true;// TODO-048: is it the best place?
//                }
//
//            }

//            cleanedPathsPerInk.put(entry.getKey(), cleanedPath);
        }
        return cleanedPathsPerInk;
    }

    /**
     * Generate the pens paths
     * 
     * @param img          the image from which to generate the paths
     * @param brushPalette the Brush Palette to use
     * @param lpmmMax      the maximum number of lines per mm
     * @param imageDpi     the image DPI
     * @param pathsPerInk  the paths per ink
     */
    private void generatePaths(Image img, BrushPalette brushPalette, double lpmmMax, int imageDpi,
            HashMap<Ink, Path> pathsPerInk) {
        double W = Utils.pxToMm(img.getWidth(), imageDpi);
        double H = Utils.pxToMm(img.getHeight(), imageDpi);
        int nbBrushes = brushPalette.getNbBrushes();
        for (int idBrush = 0; idBrush < nbBrushes; idBrush++) {
            Brush brush = brushPalette.getBrush(idBrush);
            Path path = pathsPerInk.get(brush.getInk());
            if (path == null) {
                continue;
            }
            int a = brush.getAngle();
            double angle = a * Math.PI / 180;
            double p = 1.0 / (brush.getLevel() * lpmmMax);
            if (p == 0) {
                continue;
            }
            double yFrom = angle > 0 ? 0 : W * Math.tan(angle);
            double yTo = angle > 0 ? (H + W / Math.tan(angle)) : H;
            double deltaY = p;
            if (a != 90) {
                deltaY = p / Math.cos(angle);
            } else {
                yFrom = 0;
                yTo = W;
            }
            int nbLines = (int) Math.ceil((yTo - yFrom) / deltaY);
            for (int idLine = 0; idLine < nbLines; idLine++) {
                if (shouldAbortUpdate) {
                    return;
                }
                Vector<Line> segmentsInLine = new Vector<Line>(); // TODO-048: find better names
                setProgression((double) idBrush / nbBrushes + (double) idLine / nbLines / nbBrushes, false);
                double xStart = a != 90 ? 0 : idLine * p;
                double yStart = a != 90 ? yFrom + idLine * deltaY : 0;

                double xLineStart = xStart;
                double yLineStart = yStart;
                double x, y;
                double r = 0;
                boolean isPointOutsideImage = xLineStart < 0 || yLineStart < 0 || xLineStart >= W || yLineStart >= H;
                boolean lineColored = !isPointOutsideImage && img.getPixel(Utils.mmToPx(xLineStart, imageDpi),
                        Utils.mmToPx(yLineStart, imageDpi)) == brush.getInputColor();
                while (true) {
                    r += IMAGE_TO_PATH_PRECISION_IN_MM;
                    x = xStart + r * Math.cos(angle);
                    y = yStart - r * (a != 90 ? Math.sin(angle) : -1);
                    boolean isWholeLineOutsideImage = a != 90 ? (x >= W) : (y >= H);
                    if (!isWholeLineOutsideImage) {
                        isPointOutsideImage = x < 0 || y < 0 || x >= W || y >= H;
                        boolean colored = !isPointOutsideImage && img.getPixel(Utils.mmToPx(x, imageDpi),
                                Utils.mmToPx(y, imageDpi)) == brush.getInputColor();
                        if (colored != lineColored) {
                            if (lineColored) {
                                segmentsInLine.add(new Line(xLineStart, yLineStart, x, y, idLine % 2 == 0));
                            } else {
                                xLineStart = x;
                                yLineStart = y;
                            }
                            lineColored = colored;
                        }
                    } else {
                        break;
                    }
                }
                if (lineColored) {
                    // TODO-024: better handle direction determination, we should add them in a reverse order, not just
                    // change the direction line by line
                    segmentsInLine.add(new Line(xLineStart, yLineStart, x, y, idLine % 2 == 0));
                }
                for (int idSegment = 0; idSegment < segmentsInLine.size(); idSegment++) {
                    int idSegmentToAdd = idLine % 2 == 0 ? (segmentsInLine.size() - 1 - idSegment) : idSegment;
                    path.addLine(segmentsInLine.get(idSegmentToAdd));
                }
            }
        }
    }

    @Override
    public void drawVectorizedImageOutput(Graphics2D g) {
        g.setColor(Color.black);
        Stroke defaultStroke = g.getStroke();
        g.setStroke(Utils.getPenStrokeInMm(settingsValues.getDoubleSetting(Setting.PEN_TIP_DIAMETER)));
        int imageDpi = settingsValues.getIntSetting(Setting.IMAGE_DPI);
        for (Entry<Ink, Path> entry : pathsPerInk.entrySet()) {
            g.setColor(entry.getKey().getColor());
            Path path = entry.getValue();
            for (Line line : path.getLines()) {
                g.draw(new Line2D.Double(Utils.mmToPxDouble(line.x0, imageDpi), Utils.mmToPxDouble(line.y0, imageDpi),
                        Utils.mmToPxDouble(line.x1, imageDpi), Utils.mmToPxDouble(line.y1, imageDpi)));
            }
        }
        g.setStroke(defaultStroke);
    }

    @Override
    protected Image executeTransformation(AbstractTransformation previousTransformation, SettingsSet settings) {
        int imageDpi = settings.getIntSetting(Setting.IMAGE_DPI);
        /* clear paths */
        pathsPerInk.clear();
        for (Ink ink : Ink.getAvailableInks()) {
            pathsPerInk.put(ink, new Path(settings.getDoubleSetting(Setting.CLEAR_Z_HEIGHT)));
        }

        /* generate image paths */
        Image colorQuantizedImage = Project.Instance.getTransformation(TransformationStep.COLOR_QUANTIZATION)
                .getOutputImage();
        generatePaths(colorQuantizedImage, settings.getSelectedBrushPalette(),
                settings.getDoubleSetting(Setting.LPMM_MAX), imageDpi, pathsPerInk);

        /* generate thick outline paths */
        BrushPalette outlineBrushPalette = new BrushPalette();
        for (Ink ink : Ink.getAvailableInks()) {
            outlineBrushPalette.addBrush(new Brush(ink.getColorAsRgb(), ink, 1, 45, false, false));
        }
        generatePaths(Project.Instance.getTransformation(TransformationStep.THICK_OUTLINING).getOutputImage(),
                outlineBrushPalette, settings.getDoubleSetting(Setting.OUTLINE_LPMM), imageDpi, pathsPerInk);

        /* add fine outline paths */
        FineOutliningTransformation fineOutliningTr = (FineOutliningTransformation) Project.Instance
                .getTransformation(TransformationStep.FINE_OUTLINING);
        for (Path pathInPx : fineOutliningTr.getOutlinePaths()) {
            Path pathInMm = pathInPx.convertFromPxToMm(imageDpi);
            for (Line line : pathInMm.getLines()) {
                pathsPerInk.get(Ink.getBlackestAvailableInk()).addLine(line);
            }
        }

        /* remove segments too short */
        pathsPerInk = removeSegmentsTooShort(pathsPerInk, settings.getDoubleSetting(Setting.MIN_SEGMENT_LENGTH));

        return null;
    }

}
