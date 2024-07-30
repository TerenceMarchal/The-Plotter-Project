package image_processing.engine;

import java.util.Vector;

import common.engine.Utils;
import image_processing.enums.ColorSpace;

/**
 * A class representing a color palette, i.e. a list of colors
 * 
 * @author Terence
 *
 */
public class ColorPalette {

    /**
     * The colors of the color palette
     */
    private Vector<Integer> colors;

    /**
     * Instantiate a new empty color palette
     */
    public ColorPalette() {
        colors = new Vector<Integer>();
    }

    /**
     * Add a color to the color palette
     * 
     * @param color the color to add to the color palette
     */
    public void addColor(int color) {
        if (!colors.contains(color)) {
            colors.add(color);
        }
    }

    /**
     * Get the number of colors in the color palette
     * 
     * @return the number of colors in the color palette
     */
    public int getNbColors() {
        return colors.size();
    }

    /**
     * Get the colors of the color palette
     * 
     * @return the colors of the color palette
     */
    public Vector<Integer> getColors() {
        return colors;
    }

    /**
     * Add the colors of a color palette in the color palette
     * 
     * @param palette the color palette to add to this color palette
     */
    public void addColorPalette(ColorPalette palette) {
        for (int color : palette.getColors()) {
            addColor(color);
        }
    }

    /**
     * Get the closest color of the specified color in the color palette
     * 
     * @param color      the color for which to retrieve the closest color
     * @param colorSpace the color space to use
     * @return the closest color in the color palette
     */
    public int getClosestColor(int color, ColorSpace colorSpace) {
        int distMin = Utils.distanceBetweenColors(0x000000, 0xFFFFFF, colorSpace);
        int closestColor = 0;
        for (int paletteColor : colors) {
            int dist = Utils.distanceBetweenColors(color, paletteColor, colorSpace);
            if (dist < distMin) {
                closestColor = paletteColor;
                distMin = dist;
            }
        }
        return closestColor;
    }

    @Override
    public String toString() {
        String str = "[ColorPalette:\n";
        for (int color : colors) {
            str += "0x" + Integer.toHexString(color).toUpperCase() + ",\n";
        }

        return str + "]";
    }

}
