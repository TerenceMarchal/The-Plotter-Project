package image_processing.engine;

import java.util.Comparator;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import common.engine.Ink;
import common.engine.Jsonizable;
import common.engine.Utils;
import image_processing.enums.ColorSpace;
import image_processing.enums.Setting;
import image_processing.session.Configuration;

/**
 * A class representing a Brush Palette, i.e. a list of Brushes
 * 
 * @author Terence
 *
 */
public class BrushPalette implements Jsonizable {

    /**
     * The Palette Brushes
     */
    private Vector<Brush> brushes;

    /**
     * Instantiate a new empty Brush Palette
     */
    public BrushPalette() {
        brushes = new Vector<Brush>();
    }

    /**
     * Add a Brush to the Brush Palette
     * 
     * @param brush the Brush to add to the Brush Palette
     */
    public void addBrush(Brush brush) {
        if (getBrushByInputColor(brush.getInputColor()) == null) {
            brushes.add(brush);
        } else {
            /* Note: this should never happen */
            System.err.println("BrushPalette already containing brush with input color 0x"
                    + Integer.toHexString(brush.getInputColor()) + ", ignoring it (this should not happen)");
        }
    }

    /**
     * Get the specified Brush
     * 
     * @param index the index of the Brush within the Brush Palette
     * @return the specified Brush
     */
    public Brush getBrush(int index) {
        return brushes.get(index);
    }

    /**
     * Get the Palette Brushes
     * 
     * @return the Palette Brushes
     */
    public Vector<Brush> getBrushes() {
        return brushes;
    }

    /**
     * Get the Palette Brushes sorted by input color
     * 
     * @return the Palette Brushes sorted by input color
     */
    public Vector<Brush> getBrushesSortedByInputColor() {
        Vector<Brush> sortedBrushs = (Vector<Brush>) brushes.clone();
        sortedBrushs.sort(new Comparator<Brush>() {
            @Override
            public int compare(Brush o1, Brush o2) {
                ColorSpace colorSpace = Configuration.Instance.getCurrentSettings()
                        .getColorSpaceSetting(Setting.COLOR_SPACE);
                double graySaturationThreshold = Configuration.Instance.getCurrentSettings()
                        .getDoubleSetting(Setting.GREY_SATURATION_THRESHOLD);

                // TODO-031: improve color sorting
                Ink ink1 = Utils.getClosestInk(o1.getInputColor(), colorSpace, graySaturationThreshold);
                Ink ink2 = Utils.getClosestInk(o2.getInputColor(), colorSpace, graySaturationThreshold);

                // int v1 = ink1.ordinal() << 8 | Utils.getColorComponent(o1.getInputColor(), ink1.color);
                // int v2 = ink2.ordinal() << 8 | Utils.getColorComponent(o2.getInputColor(), ink2.color);
                // return ((ink1.getR() - ink2.getR()) << 16) | ((ink1.getG() - ink2.getG()) << 8)
                // | (ink1.getB() - ink2.getB());
                return ink1.getColorAsRgb() - ink2.getColorAsRgb();
            }
        });
        return sortedBrushs;
    }

    /**
     * Get the number of Brushes in the Brush Palette
     * 
     * @return the number of Brushes in the Brush Palette
     */
    public int getNbBrushes() {
        return brushes.size();
    }

    /**
     * Get the Color Palette corresponding to the input colors
     * 
     * @return the Color Palette corresponding to the input colors
     */
    public ColorPalette getInputColorPalette() {
        ColorPalette colorPalette = new ColorPalette();
        for (Brush brush : brushes) {
            colorPalette.addColor(brush.getInputColor());
        }
        return colorPalette;
    }

    /**
     * Get the Color Palette corresponding to the output colors
     * 
     * @return the Color Palette corresponding to the output colors
     */
    public ColorPalette getOutputColorPalette() {
        // TODO-011: try to get rid of this method?
        ColorPalette colorPalette = new ColorPalette();
        colorPalette.addColor(0xFFFFFF);
        for (Brush brush : brushes) {
            if (brush.getInk() != null) {
                int color = brush.getInk().getColorAsRgb(); // TODO-011: return aimedColor instead?
                colorPalette.addColor(color);
            }
        }
        return colorPalette;
    }

    /**
     * Retrieve a Brush from its input color
     * 
     * @param inputColor the input color of the Brush to retrieve
     * @return the corresponding Brush, or null if none is corresponding
     */
    public Brush getBrushByInputColor(int inputColor) {
        for (Brush brush : brushes) {
            if (brush.getInputColor() == inputColor) {
                return brush;
            }
        }
        return null;
    }

    /**
     * Retrieve the Brushes with the specified Ink
     * 
     * @param ink the Ink of the Brushes to retrieve
     * @return the corresponding Brushes
     */
    public Vector<Brush> getBrushesOfInkColor(Ink ink) {
        Vector<Brush> brushsOfInkColor = new Vector<Brush>();
        for (Brush brush : brushes) {
            if (brush.getInk().equals(ink)) {
                brushsOfInkColor.add(brush);
            }
        }
        return brushsOfInkColor;
    }

    @Override
    public String toString() {
        String str = "[BrushPalette:\n";
        for (Brush brush : brushes) {
            str += brush.toString() + ",\n";
        }

        return str + "]";
    }

    @Override
    public JSONObject toJSonObject() {
        JSONArray brushesJsonArray = new JSONArray();
        for (int idBrush = 0; idBrush < brushes.size(); idBrush++) {
            brushesJsonArray.put(idBrush, brushes.get(idBrush).toJSonObject());
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("brushes", brushesJsonArray);
        return jsonObject;
    }

    public static Jsonizable fromJsonObject(JSONObject jsonObject) {
        BrushPalette brushPalette = new BrushPalette();
        JSONArray brushesJsonArray = jsonObject.getJSONArray("brushes");
        for (int idBrush = 0; idBrush < brushesJsonArray.length(); idBrush++) {
            brushPalette.addBrush((Brush) Brush.fromJsonObject(brushesJsonArray.getJSONObject(idBrush)));
        }
        return brushPalette;
    }

}
