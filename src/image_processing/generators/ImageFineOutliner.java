package image_processing.generators;

import java.awt.image.BufferedImage;
import java.util.Vector;

import common.engine.Line;
import common.engine.Path;
import image_processing.enums.Setting;
import image_processing.session.Configuration;

/**
 * An abstract class to generate fine outlines of images
 * 
 * @author Terence
 *
 */
public abstract class ImageFineOutliner {

    /**
     * An enumeration representing an pixel outline point type, used for the outline Paths generation
     * 
     * @author Terence
     *
     */
    private enum OutlinePointType {
        NOT_AN_OUTLINE_POINT, OUTLINE_POINT_IN_A_PATH, OUTLINE_POINT_NOT_YET_IN_A_PATH,
    }

    /**
     * Generate the fine outline Paths of the specified image
     * 
     * TODO-011: use an Image instead of a BufferedImage?
     * 
     * @param img the image to outline
     * @return the fine outline Paths of the specified image
     */
    public static Vector<Path> generateImageFineOutlinePaths(BufferedImage img) {
        if (img == null) {
            // TODO-011: check if it is still needed
            return null;
        }
        int W = img.getWidth();
        int H = img.getHeight();

        /* Find outline points column by column */
        OutlinePointType[][] outlinePointsMap = new OutlinePointType[W][H];
        for (int x = 0; x < W; x++) {
            int lastColor = img.getRGB(x, 0);
            int lastBorder = -1; // TODO-033: find a better way to handle the background borders
            for (int y = 1; y < H; y++) {
                int color = img.getRGB(x, y);
                if (color != lastColor && y - lastBorder > 1) {
                    outlinePointsMap[x][y] = OutlinePointType.OUTLINE_POINT_NOT_YET_IN_A_PATH;
                    lastBorder = y;
                }
                lastColor = color;
            }
        }

        /* Find outline points line by line */
        for (int y = 0; y < H; y++) {
            int lastColor = img.getRGB(0, y);
            int lastBorder = -1;
            for (int x = 0; x < W; x++) {
                int color = img.getRGB(x, y);
                if (color != lastColor && x - lastBorder > 1) {
                    outlinePointsMap[x][y] = OutlinePointType.OUTLINE_POINT_NOT_YET_IN_A_PATH;
                    lastBorder = x;
                }
                lastColor = color;
            }
        }

        /* Generate outline paths from outline points */
        Vector<Path> outlinePaths = new Vector<Path>();
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                if (outlinePointsMap[x][y] == OutlinePointType.OUTLINE_POINT_NOT_YET_IN_A_PATH) {
                    outlinePaths.add(generateFineOutlinePath(x, y, outlinePointsMap));
                }
            }

        }

        return outlinePaths;
    }

    /**
     * Generate a fine outline path from a starting point and a 2D map of the outline points types
     * 
     * @param xStart           the path starting point x coordinate
     * @param yStart           the path starting point y coordinate
     * @param outlinePointsMap the fine outline points types map
     * @return the generated path
     */
    private static Path generateFineOutlinePath(int xStart, int yStart, OutlinePointType[][] outlinePointsMap) {
        // TODO-033: optimize path generation by recognizing more pattern and not only checking the adjacent pixels
        Path path = new Path(Configuration.Instance.getCurrentSettings().getDoubleSetting(Setting.CLEAR_Z_HEIGHT));
        int lastX = xStart;
        int lastY = yStart;
        outlinePointsMap[xStart][yStart] = OutlinePointType.OUTLINE_POINT_IN_A_PATH;
        int W = outlinePointsMap.length;
        int H = outlinePointsMap[0].length;
        boolean continuePath = true;
        while (continuePath) {
            continuePath = false;
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    int xx = Math.max(0, Math.min(lastX + i, W - 1));
                    int yy = Math.max(0, Math.min(lastY + j, H - 1));
                    if (xx != lastX || yy != lastY) {
                        if (outlinePointsMap[xx][yy] == OutlinePointType.OUTLINE_POINT_NOT_YET_IN_A_PATH) {
                            path.addLine(new Line(lastX, lastY, xx, yy));
                            outlinePointsMap[xx][yy] = OutlinePointType.OUTLINE_POINT_IN_A_PATH;
                            lastX = xx;
                            lastY = yy;
                            continuePath = true;
                            break;
                        }
                    }
                }
                if (continuePath) {
                    break;
                }
            }
        }
        return path;
    }

}
