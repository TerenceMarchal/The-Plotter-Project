package common.engine;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.ImageIcon;

import common.enums.LabelType;
import image_processing.enums.ColorSpace;

/**
 * A class with multiple utility function, working without context
 * 
 * @author Terence
 *
 */
public abstract class Utils {

    /**
     * The screen (i.e. monitor) DPI
     */
    public static final int DISPLAY_DPI = 150;

    /**
     * Convert a px length into mm
     * 
     * @param px  the px length to convert
     * @param dpi the DPI (Dots Per Inch) to use
     * @return the corresponding length in mm
     */
    public static double pxToMm(int px, int dpi) {
        return px * 25.4 / dpi;
    }

    /**
     * Convert a mm length into px
     * 
     * @param mm  the mm length to convert
     * @param dpi the DPI (Dots Per Inch) to use
     * @return the corresponding length in px
     */
    public static int mmToPx(double mm, int dpi) {
        return (int) Math.floor(mm * dpi / 25.4);
    }

    /**
     * Convert a mm length into px, with a double precision
     * 
     * @param mm  the mm length to convert
     * @param dpi the DPI (Dots Per Inch) to use
     * @return the corresponding length in px
     */
    public static double mmToPxDouble(double mm, int dpi) {
        return mm * dpi / 25.4;
    }

    /**
     * Compute the distance between two colors in the specified color space, not taking into account the alpha channel
     * 
     * @param c0         the first color
     * @param c1         the second color
     * @param colorSpace the color space in which to compute the distance
     * @return the distance between the two colors
     */
    public static int distanceBetweenColors(int c0, int c1, ColorSpace colorSpace) {
        int r0 = (c0 & 0xFF0000) >> 16;
        int g0 = (c0 & 0x00FF00) >> 8;
        int b0 = c0 & 0x0000FF;
        int r1 = (c1 & 0xFF0000) >> 16;
        int g1 = (c1 & 0x00FF00) >> 8;
        int b1 = c1 & 0x0000FF;
        if (colorSpace == ColorSpace.RGB) {
            int R = Math.abs(r0 - r1);
            int G = Math.abs(g0 - g1);
            int B = Math.abs(b0 - b1);
            return R + G + B;
        } else if (colorSpace == ColorSpace.SRGB) {
            double rm = (r0 + r1) / 2.0;
            int R = (int) Math.pow(r0 - r1, 2);
            int G = (int) Math.pow(g0 - g1, 2);
            int B = (int) Math.pow(b0 - b1, 2);
            int d = (int) ((2 + rm / 256.0) * R + 4 * G + (2 + (255 - rm) / 256) * B);
            return d;
        } else if (colorSpace == ColorSpace.HUMAN_WEIGHTED) {
            int R = Math.abs(r0 - r1) * 30;
            int G = Math.abs(g0 - g1) * 59;
            int B = Math.abs(b0 - b1) * 11;
            return R + G + B;
        } else {
            System.err.println("Unknown color space");
            return 0;
        }
    }

    /**
     * Get the closest Ink from the specified color
     * 
     * @param color                   the color for which to find the closest Ink
     * @param colorSpace              the color space to use to compute the color distance
     * @param graySaturationThreshold the saturation threshold between 0.0 and 1.0, under which the closest Ink is
     *                                considered to be the blackest available Ink
     * @return the closest Ink
     */
    public static Ink getClosestInk(int color, ColorSpace colorSpace, double graySaturationThreshold) {
        int r = (color & 0xFF0000) >> 16;
        int g = (color & 0x00FF00) >> 8;
        int b = color & 0x0000FF;
        float[] hsb = new float[3];
        Color.RGBtoHSB(r, g, b, hsb);
        if (hsb[1] < graySaturationThreshold) {
            return Ink.getBlackestAvailableInk();
        } else {
            int smallestDist = Integer.MAX_VALUE;
            Ink closestInk = null;
            for (Ink ink : Ink.getAvailableInks()) {
                if (ink != Ink.getBlackestAvailableInk()) {
                    int dist = distanceBetweenColors(color, ink.getColorAsRgb(), colorSpace);
                    if (dist < smallestDist) {
                        closestInk = ink;
                        smallestDist = dist;
                    }
                }
            }
            return closestInk;
        }
    }

    /**
     * Get the color we aim to reproduce by drawing lines with the specified Ink and filling level
     * 
     * @param ink          the Ink used
     * @param fillingLevel the filling level, between 0.0 and 1.0
     * @return the aimed color
     */
    public static int getAimedColor(Ink ink, double fillingLevel) {
        int rInk = ink.getR();
        int gInk = ink.getG();
        int bInk = ink.getB();
        int r = rInk + (int) Math.round((255 - rInk) * (1 - fillingLevel));
        int g = gInk + (int) Math.round((255 - gInk) * (1 - fillingLevel));
        int b = bInk + (int) Math.round((255 - bInk) * (1 - fillingLevel));
        return r << 16 | g << 8 | b;
    }

    /**
     * Get the Jave AWT Stroke corresponding to the specified pen width in mm and screen DPI
     * 
     * @param penWidthInMm the pen tip width in mm
     * @param screenDpi    the screen's DPI
     * @return the corresponding Stroke
     */
    public static Stroke getPenStrokeInMm(double penWidthInMm) {
        return new BasicStroke((float) mmToPxDouble(penWidthInMm, DISPLAY_DPI), BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND);
    }

    /**
     * Get the Jave AWT Stroke corresponding to the specified pen width in px
     * 
     * @param penWidthInPx the pen tip width in px
     * @return the corresponding Stroke
     */
    public static Stroke getPenStrokeInPx(double penWidthInPx) {
        return new BasicStroke((float) penWidthInPx, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    /**
     * Get the factorial of n
     * 
     * @param n the factorial to compute
     * @return n!
     */
    public static long factorial(int n) {
        long res = 1;
        for (int i = 2; i <= n; i++) {
            res *= i;
        }
        return res;
    }

    /**
     * Compute the N choose K
     * 
     * @param n the total number of elements
     * @param k the number of elements to select
     * @return the number of combination of K elements in a pool of N elements
     */
    public static int nChooseK(int n, int k) {
        return (int) (factorial(n) / (factorial(n - k) * factorial(k)));
    }

    /**
     * Get the squared distance between two points
     * 
     * @param x0 the start point X coordinate
     * @param y0 the start point Y coordinate
     * @param x1 the end point X coordinate
     * @param y1 the end point Y coordinate
     * @return
     */
    public static double getSquaredDist(double x0, double y0, double x1, double y1) {
        return Math.pow(x1 - x0, 2) + Math.pow(y1 - y0, 2);
    }

    /**
     * Get an array of all the possible combinations of the specified length of the specified values
     * 
     * @param values                     the values from which computing the combinations
     * @param lengthOfCombinationsToKeep the length of the combinations to compute
     * @return an array of all the possible combinations of the specified length of the specified values
     */
    public static int[][] getCombinationsOfLength(Vector<Integer> values, int lengthOfCombinationsToKeep) {
        int[][] combinations = new int[nChooseK(values.size(), lengthOfCombinationsToKeep)][lengthOfCombinationsToKeep];
        int idCombination = 0;
        for (int i = 1; i < Math.pow(2, values.size()); i++) {
            // TODO-035: optimize without using an ArrayList
            ArrayList<Integer> combination = new ArrayList<Integer>();
            for (int j = 0; j < values.size(); j++) {
                if ((i & (long) Math.pow(2, j)) > 0) {
                    combination.add(values.get(j));
                }
            }
            if (combination.size() == lengthOfCombinationsToKeep) {
                for (int idValue = 0; idValue < combination.size(); idValue++) {
                    combinations[idCombination][idValue] = combination.get(idValue);
                }
                idCombination++;
            }
        }
        return combinations;
    }

    /**
     * Generate an ImageIcon of a simple square filled with the specified color
     * 
     * @param color the color with which to fill the icon
     * @return an ImageIcon of the specified color
     */
    public static ImageIcon getColorIcon(Color color) {
        int w = 12;
        BufferedImage img = new BufferedImage(w, w, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        g.setColor(color);
        g.fillRect(0, 0, w, w);
        return new ImageIcon(img);
    }

    /**
     * Generate a lighter version of the specified color
     * 
     * @param c the color from which to generate a lighter version
     * @return the lighter color
     */
    public static Color getLighterColor(Color c) {
        return new Color((2 * 0xFF + c.getRed()) / 3, (2 * 0xFF + c.getGreen()) / 3, (2 * 0xFF + c.getBlue()) / 3);
    }

    /**
     * Get the best foreground color (between black and white) according to the specified background color
     * 
     * @param backgroundColor the color of the background on which the foreground color should be displayed
     * @return the best foreground color
     */
    public static Color getBestForeground(Color backgroundColor) {
        return (backgroundColor.getRed() + backgroundColor.getGreen() + backgroundColor.getBlue()) / 3 >= 128
                ? Color.WHITE
                : Color.BLACK;
    }

    /**
     * Generate an HTML tooltip text of the specified label type with the specified double values per Ink
     * 
     * @param valuesPerInk the values per Ink with which to generate the tooltip
     * @param labelType    the label type of the tooltip
     * @return the corresponding HTML tooltip text
     */
    public static String generateDoubleValuePerInkTooltip(HashMap<Ink, Double> valuesPerInk, LabelType labelType) {
        String tooltip = "<html>";
        for (Ink ink : Ink.getAvailableInks()) {
            if (!valuesPerInk.containsKey(ink)) {
                continue;
            }
            double value = valuesPerInk.get(ink);
            String beautifiedValue = value + "";
            switch (labelType) {
            case DISTANCE:
                beautifiedValue = beautifyDistance(value);
                break;
            case DURATION:
                beautifiedValue = beautifyDuration(value);
                break;
            case DURATION_AS_CLOCK:
                beautifiedValue = beautifyDurationAsClock(value);
                break;
            }
            tooltip += String.format(Locale.US, "<span style='color:%s'>%s for %s ink</span><br/>",
                    ink.getColorAsHexString(), beautifiedValue, ink.getName());
        }
        tooltip += "</html>";
        return tooltip;
    }

    /**
     * Generate an HTML tooltip text of the specified label type with the specified int values per Ink
     * 
     * @param valuesPerInk the values per Ink with which to generate the tooltip
     * @param labelType    the label type of the tooltip
     * @return the corresponding HTML tooltip text
     */
    public static String generateIntValuePerInkTooltip(HashMap<Ink, Integer> valuesPerInk, LabelType labelType) {
        HashMap<Ink, Double> doubleValuesPerInk = new HashMap<Ink, Double>();
        for (Entry<Ink, Integer> entry : valuesPerInk.entrySet()) {
            doubleValuesPerInk.put(entry.getKey(), Double.valueOf(entry.getValue()));
        }
        return generateDoubleValuePerInkTooltip(doubleValuesPerInk, labelType);
    }

    /**
     * Generate an HTML tooltip text of the specified label type with the specified progressions per Ink
     * 
     * @param currentValuesPerInk the current values per Ink with which to generate the tooltip
     * @param valuesMaxPerInk     the maximum values per Ink with which to generate the tooltip
     * @param labelType           the label type of the tooltip
     * @return the corresponding HTML tooltip text
     */
    public static String generateDoubleProgressionPerInkTooltip(HashMap<Ink, Double> currentValuesPerInk,
            HashMap<Ink, Double> valuesMaxPerInk, LabelType labelType) {
        String tooltip = "<html>";
        for (Ink ink : Ink.getAvailableInks()) {
            if (!currentValuesPerInk.containsKey(ink)) {
                continue;
            }
            String beautifiedValue = beautifyProgression(currentValuesPerInk.get(ink), valuesMaxPerInk.get(ink),
                    labelType);
            tooltip += String.format(Locale.US, "<span style='color:%s'>%s for %s ink</span><br/>",
                    ink.getColorAsHexString(), beautifiedValue, ink.getName());
        }
        tooltip += "</html>";
        return tooltip;
    }

    /**
     * Generate an HTML tooltip text of the specified label type with the specified progressions per Ink
     * 
     * @param currentValuesPerInk the current values per Ink with which to generate the tooltip
     * @param valuesMaxPerInk     the maximum values per Ink with which to generate the tooltip
     * @param labelType           the label type of the tooltip
     * @return the corresponding HTML tooltip text
     */
    public static String generateIntProgressionPerInkTooltip(HashMap<Ink, Integer> currentValuesPerInk,
            HashMap<Ink, Integer> valuesMaxPerInk, LabelType labelType) {
        HashMap<Ink, Double> doubleCurrentValuesPerInk = new HashMap<Ink, Double>();
        HashMap<Ink, Double> doubleValuesMaxPerInk = new HashMap<Ink, Double>();
        for (Ink ink : currentValuesPerInk.keySet()) {
            doubleCurrentValuesPerInk.put(ink, Double.valueOf(currentValuesPerInk.get(ink)));
            doubleValuesMaxPerInk.put(ink, Double.valueOf(valuesMaxPerInk.get(ink)));
        }
        return generateDoubleProgressionPerInkTooltip(doubleCurrentValuesPerInk, doubleValuesMaxPerInk, labelType);
    }

    /**
     * Generate a beautified string of the specified distance in mm
     * 
     * @param distanceInMm the distance to beautify, in mm
     * @return the distance as a beautified string
     */
    public static String beautifyDistance(double distanceInMm) {
        if (distanceInMm < 1000) {
            return String.format(Locale.US, "%.0fmm", distanceInMm);
        } else {
            return String.format(Locale.US, "%.1fm", distanceInMm / 1000);
        }
    }

    /**
     * Generate a beautified string of the specified duration in seconds
     * 
     * @param durationInS the duration to beautify, in seconds
     * @return the duration as a beautified string
     */
    public static String beautifyDuration(double durationInS) {
        if (durationInS < 60) {
            return String.format(Locale.US, "%.0fs", durationInS);
        } else if (durationInS < 3600) {
            return String.format(Locale.US, "%dmn %ds", (int) (durationInS / 60), (int) (durationInS % 60));
        } else {
            return String.format(Locale.US, "%dh %dmn %ds", (int) (durationInS / 3600),
                    (int) ((durationInS % 3600) / 60), (int) (durationInS % 60));
        }
    }

    /**
     * Generate a clock-like beautified string of the specified duration in seconds
     * 
     * @param durationInS the duration to beautify, in seconds
     * @return the duration as a clock-like beautified string
     */
    public static String beautifyDurationAsClock(double durationInS) {
        return String.format(Locale.US, "%d:%02d:%02d", (int) durationInS / 3600, (int) (durationInS % 3600) / 60,
                (int) durationInS % 60);
    }

    /**
     * Generate a beautified string of the specified progression
     * 
     * @param currentValue the current value of the progression
     * @param maxValue     the maximum value of the progression
     * @param labelType    the label type of the progression
     * @return the progression as a beautified string
     */
    public static String beautifyProgression(double currentValue, double maxValue, LabelType labelType) {
        switch (labelType) {
        case DISTANCE:
            return String.format(Locale.US, "%s/%s", beautifyDistance(currentValue), beautifyDistance(maxValue));
        default:
            return String.format(Locale.US, "%.0f/%.0f", currentValue, maxValue);
        }
    }

    /**
     * Read a whole file and return its content as a string
     * 
     * @param file the file to read
     * @return the file content
     * @throws IOException any exception thrown during the reading of the file
     */
    public static String readFile(File file) throws IOException {
        String content = "";
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            content += line + "\n";
        }
        reader.close();
        return content;
    }

    /**
     * Compute the estimated travel duration between the two set of coordinates
     * 
     * @param x0           the start point X coordinate
     * @param y0           the start point Y coordinate
     * @param z0           the start point Z coordinate
     * @param x1           the end point X coordinate
     * @param y1           the end point Y coordinate
     * @param z1           the end point Z coordinate
     * @param accelXY      the Plotter acceleration on the X/Y axis, in mm/s^2
     * @param speedMaxXY   the Plotter maximum speed on the X/Y axis, in mm/s
     * @param accelZ       the Plotter acceleration on the Z axis, in mm/s^2
     * @param speedMaxZ    the Plotter maximum speed on the Z axis, in mm/s
     * @param coreXyDesign indicate if the Plotter is based on a CoreXY design
     * @return the estimated travel duration
     */
    public static double computeXYZTravelDuration(double x0, double y0, double z0, double x1, double y1, double z1,
            double accelXY, double speedMaxXY, double accelZ, double speedMaxZ, boolean coreXyDesign) {
        double durationX, durationY, durationZ;
        double distX, distY, distZ;
        if (coreXyDesign) {
            double angle = Math.atan2(Math.abs(y1 - y0), Math.abs(x1 - x0));
            double alpha = Math.cos(angle - Math.PI / 4);
            distX = Math.abs(x1 - x0) / alpha + Math.abs(y1 - y0) / alpha;
            distY = 0;
            speedMaxXY /= alpha;
        } else {
            distX = Math.abs(x1 - x0);
            distY = Math.abs(y1 - y0);
        }
        distZ = Math.abs(z1 - z0);

        double distanceToReachMaxXYSpeed = 0.5 / accelXY * speedMaxXY * speedMaxXY;
        if (distX < 2 * distanceToReachMaxXYSpeed) {
            durationX = 2 * Math.sqrt(distX / accelXY);
        } else {
            durationX = 2 * speedMaxXY / accelXY + (distX - 2 * distanceToReachMaxXYSpeed) / speedMaxXY;
        }
        if (distY < 2 * distanceToReachMaxXYSpeed) {
            durationY = 2 * Math.sqrt(distY / accelXY);
        } else {
            durationY = 2 * speedMaxXY / accelXY + (distY - 2 * distanceToReachMaxXYSpeed) / speedMaxXY;
        }
        double distanceToReachMaxZSpeed = 0.5 / accelZ * speedMaxZ * speedMaxZ;
        if (distZ < 2 * distanceToReachMaxZSpeed) {
            durationZ = 2 * Math.sqrt(distZ / accelZ);
        } else {
            durationZ = 2 * speedMaxZ / accelZ + (distZ - 2 * distanceToReachMaxZSpeed) / speedMaxZ;
        }
        return Math.max(durationX, Math.max(durationY, durationZ));
    }

    /**
     * Compute the travel duration between two coordinate on the same Z plan
     * 
     * @param x0           the start point X coordinate
     * @param y0           the start point Y coordinate
     * @param x1           the end point X coordinate
     * @param y1           the end point Y coordinate
     * @param accel        the Plotter acceleration on the X/Y axis, in mm/s^2
     * @param speedMax     the Plotter maximum speed on the X/Y axis, in mm/s
     * @param coreXyDesign indicate if the Plotter is based on a CoreXY design
     * @return the estimated travel duration
     */
    public static double computeXYTravelDuration(double x0, double y0, double x1, double y1, double accel,
            double speedMax, boolean coreXyDesign) {
        return computeXYZTravelDuration(x0, y0, 0, x1, y1, 0, accel, speedMax, 1, 1, coreXyDesign);
    }

    /**
     * Compute the travel duration between two coordinate on the same Z axis
     * 
     * @param z0           the start point Z coordinate
     * @param z1           the end point Z coordinate
     * @param accel        the Plotter acceleration on the Z axis, in mm/s^2
     * @param speedMax     the Plotter maximum speed on the Z axis, in mm/s
     * @param coreXyDesign indicate if the Plotter is based on a CoreXY design
     * @return the estimated travel duration
     */
    public static double computeZTravelDuration(double z0, double z1, double accel, double speedMax,
            boolean coreXyDesign) {
        return computeXYZTravelDuration(0, 0, z0, 0, 0, z1, 1, 1, accel, speedMax, coreXyDesign);
    }

    /**
     * Convert a speed in mm/s into a feedrate, depending on the Plotter design
     * 
     * @param speed    the speed to convert, in mm/s
     * @param isCoreXy true if the Plotter is based on a CoreXY design, false otherwise
     * @return the converted feedrate
     */
    public static double speedToFeedrate(double speed, boolean isCoreXy) {
        double feedrate = speed * 60;
        if (isCoreXy) {
            feedrate /= Math.cos(Math.PI / 4);
        }
        return feedrate;
    }
}
