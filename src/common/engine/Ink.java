package common.engine;

import java.awt.Color;
import java.util.Vector;

import org.json.JSONObject;

import image_processing.enums.ColorSpace;

/**
 * An Ink, used by the Plotter for the drawings
 * 
 * @author Terence
 *
 */
public class Ink implements Jsonizable {

    /**
     * The available Inks
     */
    private static Vector<Ink> availableInks = new Vector<Ink>();

    /**
     * The "blackest" available Ink, i.e. the one that is closer to absolute black (#000000))
     */
    private static Ink blackestAvailableInk;

    /**
     * The Ink name;
     */
    private String name;

    /**
     * The Ink color
     */
    private int color;

    /**
     * Instantiate a new Ink
     * 
     * @param color the Ink name
     * @param color the Ink color
     */
    private Ink(String name, int color) {
        this.name = name;
        this.color = color;
    }

    /**
     * Get the Ink name
     * 
     * @return the Ink name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the Ink color
     * 
     * @return the Ink color
     */
    public Color getColor() {
        return new Color(color);
    }

    /**
     * Get the Ink color as its RGB value
     * 
     * @return the Ink color as its RGB value
     */
    public int getColorAsRgb() {
        return color;
    }

    /**
     * Get the Ink color as an hexadecimal string
     * 
     * @return the Ink color as an hexadecimal string
     */
    public String getColorAsHexString() {
        return "#" + Integer.toHexString(color).toUpperCase();
    }

    /**
     * Get the Ink color red component
     * 
     * @return the Ink color red component
     */
    public int getR() {
        return (color >> 16) & 0xFF;
    }

    /**
     * Get the Ink color green component
     * 
     * @return the Ink color green component
     */
    public int getG() {
        return (color >> 8) & 0xFF;
    }

    /**
     * Get the Ink color blue component
     * 
     * @return the Ink color blue component
     */
    public int getB() {
        return color & 0xFF;
    }

    /**
     * Indicate if this Ink is the blackest available Ink
     * 
     * @return true if this Ink is the blackest available Ink, false otherwise
     */
    public boolean isBlackestAvailableInk() {
        return this.equals(blackestAvailableInk);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Ink) {
            Ink other = (Ink) o;
            return color == other.color;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + new Integer(color).hashCode();
        hash = 31 * hash + name.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return String.format("[Ink: %s (%s)%s]", name, getColorAsHexString(),
                isBlackestAvailableInk() ? " (blackest ink available)" : "");
    }

    /**
     * Register a new Ink as an available Ink
     * 
     * @param ink the Ink to register
     * @return true if the Ink was registered successfully, false otherwise, if an Ink with the same color was already
     *         registered
     */
    public static boolean registerNewAvailableInk(Ink ink) {
        for (Ink availableInk : availableInks) {
            if (availableInk.getColor() == ink.getColor()) {
                return false;
            }
        }
        availableInks.add(ink);

        // TODO-051: use the configured ColorSpace for determining the blackest ink?
        int blackestInkDistanceToAbsoluteBlack = blackestAvailableInk != null
                ? Utils.distanceBetweenColors(0x000000, blackestAvailableInk.getColorAsRgb(), ColorSpace.RGB)
                : Integer.MAX_VALUE;
        int inkDistanceToAbsoluteBlack = Utils.distanceBetweenColors(0x000000, ink.getColorAsRgb(), ColorSpace.RGB);
        if (inkDistanceToAbsoluteBlack < blackestInkDistanceToAbsoluteBlack) {
            blackestAvailableInk = ink;
        }

        return true;
    }

    /**
     * Get the number of available Inks
     * 
     * @return the number of available Inks
     */
    public static int getNbAvailableInks() {
        return availableInks.size();
    }

    /**
     * Get the available Inks
     * 
     * TODO-031: return them sorted somehow?
     * 
     * @return the available Inks
     */
    public static Vector<Ink> getAvailableInks() {
        return availableInks;
    }

    /**
     * Get the blackest available Ink
     * 
     * @return the blackest available ink
     */
    public static Ink getBlackestAvailableInk() {
        return blackestAvailableInk;
    }

    /**
     * Retrieve the available Ink corresponding to the specified color
     * 
     * @param color the color for which to retrieve the Ink
     * @return the corresponding Ink, or null if there is none
     */
    public static Ink getAvailableInkByColor(int color) {
        for (Ink inkColor : availableInks) {
            if (inkColor.color == color) {
                return inkColor;
            }
        }
        return null;
    }

    /**
     * Retrieve the available Ink corresponding to the specified color
     * 
     * @param color the color for which to retrieve the Ink
     * @return the corresponding Ink, or null if there is none
     */
    public static Ink getAvailableInkByName(String name) {
        for (Ink inkColor : availableInks) {
            if (inkColor.name.equals(name)) {
                return inkColor;
            }
        }
        return null;
    }

    @Override
    public JSONObject toJSonObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("color", getColorAsHexString());
        return jsonObject;
    }

    public static Jsonizable fromJsonObject(JSONObject jsonObject) {
        return new Ink(jsonObject.getString("name"), Integer.parseInt(jsonObject.getString("color").substring(1), 16));
    }
}
