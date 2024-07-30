package image_processing.generators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import image_processing.engine.ColorPalette;
import image_processing.engine.Image;
import image_processing.enums.ColorQuantizationMethod;
import image_processing.enums.ColorSelectionMethod;

/**
 * An abstract class used to generate ColorPalettes
 * 
 * @author Terence
 *
 */
public abstract class ColorPaletteGenerator {

    /**
     * Generate a ColorPalette with a reduced number of colors from the specified pixels colors
     * 
     * @param pixelsColors            a list containing the pixels colors from which to generate the reduced color
     *                                palette
     * @param nbColors                the number of colors of the palette
     * @param colorQuantizationMethod the color quantization method to use to generate the reduced color palette
     * @param colorSelectionMethod    the color selection method to use to generate the reduced color palette
     * @return the reduced color palette
     */
    private static ColorPalette generateReducedColorPalette(ArrayList<Integer> pixelsColors, int nbColors,
            ColorQuantizationMethod colorQuantizationMethod, ColorSelectionMethod colorSelectionMethod) {
        if (colorQuantizationMethod == ColorQuantizationMethod.FAVOR_COLORS_TRUE_TO_ORIGINALS) {
            int idFirstSetBit = Integer.toBinaryString(nbColors).indexOf('1');
            int idLastSetBit = Integer.toBinaryString(nbColors).lastIndexOf('1');
            if (idFirstSetBit != idLastSetBit) {
                nbColors = 1 << (Integer.toBinaryString(nbColors).length() - 1 - idFirstSetBit);
                System.err.println(
                        "Warning: if recursive cut is used then nbColors should be a power of 2, using instead "
                                + nbColors);
            }
            nbColors /= 2;
        } else if (colorQuantizationMethod == ColorQuantizationMethod.FAVOR_CONTRASTS) {
            nbColors--;
        } else {
            System.err.println("Unsupported color quantization method: " + colorQuantizationMethod);
            return null;
        }
        return cutPalette(new ColorPalette(), pixelsColors, nbColors, colorQuantizationMethod, colorSelectionMethod);
    }

    // TODO-011: clean that function, rename it and add doc
    private static ColorPalette cutPalette(ColorPalette colorPalette, List<Integer> pixels, int nbCuts,
            ColorQuantizationMethod colorQuantizationMethod, ColorSelectionMethod colorSelectionMethod) {
        if (pixels.size() == 0) { // TODO-011: remove that? probably an issue with the cut
            System.err.println("error: tried to cut empty palette");
            return colorPalette;
        }
        if (nbCuts > 0) {
            /* split pixel buck in two */
            // TODO-011: clean that function
            int minR = 0xFF0000, minG = 0x00FF00, minB = 0x0000FF;
            int maxR = 0, maxG = 0, maxB = 0;
            for (int i = 0; i < pixels.size(); i++) {
                int px = pixels.get(i);
                int r = px & 0xFF0000;
                int g = px & 0x00FF00;
                int b = px & 0x0000FF;
                if (r < minR) {
                    minR = r;
                }
                if (r > maxR) {
                    maxR = r;
                }
                if (g < minG) {
                    minG = g;
                }
                if (g > maxG) {
                    maxG = g;
                }
                if (b < minB) {
                    minB = b;
                }
                if (b > maxB) {
                    maxB = b;
                }
            }
            int deltaR = (maxR - minR) >> 16;
            int deltaG = (maxG - minG) >> 8;
            int deltaB = maxB - minB;
            int filter = 0;

            if (deltaR >= deltaG && deltaR >= deltaB) {
                filter = 0xFF0000;
            } else if (deltaG >= deltaR && deltaG >= deltaB) {
                filter = 0x00FF00;
            } else {
                filter = 0x0000FF;
            }
            int filterFinal = filter;
            Collections.sort(pixels, new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    return (o1 & filterFinal) - (o2 & filterFinal); // TODO-011: can we use a single &?
                }
            });
            // TODO-011: is a new arraylist necessary?
            if (colorQuantizationMethod == ColorQuantizationMethod.FAVOR_COLORS_TRUE_TO_ORIGINALS) {
                cutPalette(colorPalette, new ArrayList(pixels.subList(0, pixels.size() / 2)), nbCuts / 2,
                        colorQuantizationMethod, colorSelectionMethod);
                cutPalette(colorPalette, new ArrayList(pixels.subList(pixels.size() / 2, pixels.size())), nbCuts / 2,
                        colorQuantizationMethod, colorSelectionMethod);
            } else if (colorQuantizationMethod == ColorQuantizationMethod.FAVOR_CONTRASTS) {
                cutPalette(colorPalette, new ArrayList(pixels.subList(pixels.size() / 2, pixels.size())), 0,
                        colorQuantizationMethod, colorSelectionMethod);
                cutPalette(colorPalette, new ArrayList(pixels.subList(0, pixels.size() / 2)), nbCuts - 1,
                        colorQuantizationMethod, colorSelectionMethod);
            } else {
                System.err.println("Unsupported color quantization method: " + colorQuantizationMethod);
                return null;
            }
        } else {
            int color = 0;
            if (colorSelectionMethod == ColorSelectionMethod.USE_MEDIAN_COLOR) {
                /* compute bucket median color */
                for (int comp = 0; comp < 3; comp++) {
                    int filter = 0xFF << (comp * 8);
                    Collections.sort(pixels, new Comparator<Integer>() {

                        @Override
                        public int compare(Integer o1, Integer o2) {
                            // TODO-051: we should use the color space here
                            return (o1 & filter) - (o2 & filter);
                        }
                    });
                    color |= pixels.get(pixels.size() / 2) & filter;
                }
            } else {

                /* compute bucket average color */
                long r = 0;
                long g = 0;
                long b = 0;
                for (int i = 0; i < pixels.size(); i++) {
                    int px = pixels.get(i);
                    r += (px & 0xFF0000) >> 16;
                    g += (px & 0x00FF00) >> 8;
                    b += px & 0x0000FF;
                }
                r /= pixels.size();
                g /= pixels.size();
                b /= pixels.size();
                color = (int) (r << 16 | g << 8 | b);
            }
            colorPalette.addColor(color);
        }
        return colorPalette;
    }

    /**
     * Compute a ColorPalette with a reduced number of colors from the specified image
     * 
     * @param sourceImg               the source image from which to generate the reduced color palette
     * @param nbColors                the number of colors of the palette
     * @param colorQuantizationMethod the color quantization method to use to generate the reduced color palette
     * @param colorSelectionMethod    the color selection method to use to generate the reduced color palette
     * @return the reduced color palette
     */
    public static ColorPalette generateReducedPaletteColorFromImage(Image sourceImg, int nbColors,
            ColorQuantizationMethod colorQuantizationMethod, ColorSelectionMethod colorSelectionMethod) {
        ArrayList<Integer> pixelsList = new ArrayList<Integer>();
        for (int x = 0; x < sourceImg.getWidth(); x++) {
            for (int y = 0; y < sourceImg.getHeight(); y++) {
                pixelsList.add(sourceImg.getPixel(x, y));
            }
        }
        return generateReducedColorPalette(pixelsList, nbColors, colorQuantizationMethod, colorSelectionMethod);
    }

}
