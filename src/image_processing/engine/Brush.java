package image_processing.engine;

import org.json.JSONObject;

import common.engine.Ink;
import common.engine.Jsonizable;
import common.engine.Utils;

/**
 * A class representing a Brush, containing all the settings needed to transform pixels of an input color into G-Code
 * instructions to draw the pixels with a real pen
 * 
 * @author Terence
 *
 */
public class Brush implements Jsonizable {

    /**
     * The input pixel color
     */
    private final int inputColor;

    /**
     * The aimed output color
     */
    private final int aimedOutputColor;

    /**
     * The Ink used for the real-life drawing
     */
    private final Ink ink;

    /**
     * The filling level, between 0.0 (no ink is drawed) and 1.0 (no free space is left between pen strokes)
     */
    private final double level;

    /**
     * The angle at which the pen strokes are drawed
     */
    private final int angle;

    /**
     * Is a fine outline of the input color area enabled
     */
    private final boolean fineOutliningEnabled;

    /**
     * Is a thick outline of the input color area enabled
     */
    private final boolean thickOutliningEnabled;

    /**
     * Instantiate a new Brush
     * 
     * @param inputColor            the input color, i.e. the color of pixels on which this Brush will be applied
     * @param ink                   the Ink used by this Brush
     * @param level                 the filling level used by this Brush
     * @param angle                 the angle at which the pen stroke are drawed with this Brush
     * @param fineOutliningEnabled  true if a fine outline of the input color area is enabled, false otherwise
     * @param thickOutliningEnabled true if a thick outline of the input color area is enabled, false otherwise
     */
    public Brush(int inputColor, Ink ink, double level, int angle, boolean fineOutliningEnabled,
            boolean thickOutliningEnabled) {
        this.inputColor = inputColor;
        this.ink = ink;
        if (level > 1) {
            System.err.println("level should be <=1");
            level = 1;
        }
        this.level = level;
        this.angle = angle;
        this.fineOutliningEnabled = fineOutliningEnabled;
        this.thickOutliningEnabled = thickOutliningEnabled;
        aimedOutputColor = ink != null ? Utils.getAimedColor(ink, level) : 0xFFFFFF;
    }

    /**
     * Get the input color of the Brush
     * 
     * @return the input color of the Brush
     */
    public int getInputColor() {
        return inputColor;
    }

    /**
     * Get the aimed color of the Brush
     * 
     * @return the aimed color of the Brush
     */
    public int getAimedOutputColor() {
        return aimedOutputColor;
    }

    /**
     * Get the Ink of the Brush
     * 
     * @return the Ink of the Brush
     */
    public Ink getInk() {
        return ink;
    }

    /**
     * Get the filling level of the Brush
     * 
     * @return the filling level of the Brush
     */
    public double getLevel() {
        return level;
    }

    /**
     * Get the pen stroke angle of the Brush
     * 
     * @return the pen stroke angle level of the Brush
     */
    public int getAngle() {
        return angle;
    }

    /**
     * Indicate if a fine outline of the input color area is enabled
     * 
     * @return true if a fine outline of the input color area is enabled, false otherwise
     */
    public boolean isFineOutliningEnabled() {
        return fineOutliningEnabled;
    }

    /**
     * Indicate if a thick outline of the input color area is enabled
     * 
     * @return true if a thick outline of the input color area is enabled, false otherwise
     */
    public boolean isThickOutliningEnabled() {
        return thickOutliningEnabled;
    }

    @Override
    public Brush clone() {
        return new Brush(inputColor, ink, level, angle, fineOutliningEnabled, thickOutliningEnabled);
    }

    @Override
    public String toString() {
        return "#" + Integer.toHexString(inputColor).toUpperCase() + "-" + ink.getName() + "-"
                + (int) Math.round(level * 100) + "percent";
    }

    @Override
    public JSONObject toJSonObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("inputColor", inputColor);
        jsonObject.put("ink", ink.getName());
        jsonObject.put("level", level);
        jsonObject.put("angle", angle);
        jsonObject.put("fineOutliningEnabled", fineOutliningEnabled);
        jsonObject.put("thickOutliningEnabled", thickOutliningEnabled);
        return jsonObject;
    }

    public static Jsonizable fromJsonObject(JSONObject jsonObject) {
        return new Brush(jsonObject.getInt("inputColor"), Ink.getAvailableInkByName(jsonObject.getString("ink")),
                jsonObject.getDouble("level"), jsonObject.getInt("angle"),
                jsonObject.getBoolean("fineOutliningEnabled"), jsonObject.getBoolean("thickOutliningEnabled"));
    }

}
