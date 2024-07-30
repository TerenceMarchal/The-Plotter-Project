package image_processing.transformations;

import java.awt.image.BufferedImage;

import common.engine.SettingsSet;
import image_processing.engine.Image;
import image_processing.enums.Setting;

/**
 * A Transformation to thin (i.e. skeletonize) an image
 * 
 * TODO-024: implement this algorithm in the transformation flow and check that it works as expected
 * 
 * @author Terence
 *
 */
public class ThinningTransformation extends AbstractTransformation {

    /**
     * Instantiate a Thinning Transformation
     */
    public ThinningTransformation() {
        super(TransformationStep.THINNING, new Setting[] {}, false);
    }

    /**
     * Get the number of black neighboring pixels at the specified position
     * 
     * Note: assume that the pixel has 8 neighbours
     * 
     * @param pixelsMap the pixels map, true being a black pixel and false a white pixel
     * @param x         the x position at which to count the black neighbours
     * @param y         the y position at which to count the black neighbours
     * @return the number of black neighboring pixels at the specified position
     */
    private int getNbBlackNeighbours(boolean[][] pixelsMap, int x, int y) {
        //
        int nbBlackNeighbours = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i != 0 || j != 0 && pixelsMap[x + i][y + j]) {
                    nbBlackNeighbours++;
                }
            }
        }
        return nbBlackNeighbours;
    }

    /**
     * Get the number of color transitions around the specified position
     * 
     * Note: assume that the pixel has 8 neighbours
     * 
     * @param pixelsMap the pixels map, true being a black pixel and false a white pixel
     * @param x         the x position at which to count the black neighbours
     * @param y         the y position at which to count the black neighbours
     * @return the number of color transitions around the specified position
     */
    private int getNbColorTransitions(boolean[][] pixels, int x, int y) {
        int lastColor = -1;
        int nbColorTransitions = 0;
        int[] dx = new int[] { -1, -1, 0, 1, 1, 1, 0, -1 };
        int[] dy = new int[] { 0, -1, -1, -1, 0, 1, 1, 1 };
        for (int i = 0; i < 9; i++) {
            int id = i & 0b111;
            int color = pixels[x + dx[id]][y + dy[id]] ? 1 : 0;
            if (lastColor == -1) {
                lastColor = color;
            } else if (color != lastColor) {
                nbColorTransitions++;
                lastColor = color;
            }
        }
        return nbColorTransitions;
    }

    @Override
    protected Image executeTransformation(AbstractTransformation previousTransformation, SettingsSet settings) {
        Image sourceImg = previousTransformation.getOutputImage();
        int W = sourceImg.getWidth();
        int H = sourceImg.getHeight();
        boolean[][] pixels = new boolean[W][H]; /* true for black pixels, false for whites */
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                pixels[x][y] = sourceImg.getPixel(x, y) != 0xFFFFFF;
            }
        }

        int nbPixelsRemoved = 1;
        while (nbPixelsRemoved > 0) {
            nbPixelsRemoved = 0;
            for (int idPass = 0; idPass < 2; idPass++) {
                for (int x = 1; x < W - 1; x++) {
                    for (int y = 1; y < H - 1; y++) {
                        if (pixels[x][y]) {
                            int nbBlackNeighbours = getNbBlackNeighbours(pixels, x, y);
                            if (nbBlackNeighbours >= 2 && nbBlackNeighbours <= 6) {
                                if (getNbColorTransitions(pixels, x, y) == 1) {
                                    if (idPass == 1) {
                                        /* at least one white on N, E or S */
                                        if (!pixels[x][y - 1] || !pixels[x - 1][y] || !pixels[x][y + 1]) {
                                            /* at least one white on E, S or W */
                                            if (!pixels[x + 1][y] || !pixels[x][y + 1] || !pixels[x - 1][y]) {
                                                pixels[x][y] = false;
                                                nbPixelsRemoved++;
                                            }
                                        }
                                    } else {
                                        /* at least one white on N, E or W */
                                        if (!pixels[x][y - 1] || !pixels[x - 1][y] || !pixels[x + 1][y]) {
                                            /* at least one white on N, S or W */
                                            if (!pixels[x][y - 1] || !pixels[x][y + 1] || !pixels[x - 1][y]) {
                                                pixels[x][y] = false;
                                                nbPixelsRemoved++;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        BufferedImage outputImg = new BufferedImage(sourceImg.getWidth(), sourceImg.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                outputImg.setRGB(x, y, pixels[x][y] ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return new Image(outputImg);
    }

}
