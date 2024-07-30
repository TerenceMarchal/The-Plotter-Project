package streaming.engine;

import java.util.Locale;

import org.json.JSONObject;

import common.engine.Ink;
import common.engine.Jsonizable;

/**
 * A class representing a Plotter Tool (as of now, a simple pen, latter it could be a brush)
 * 
 * @author Terence
 *
 */
public class Tool implements Jsonizable {

    /**
     * An undefined tool, meaning an unknown tool (or no tool at all)
     */
    public static final Tool UNDEFINED = new Tool(true, false);

    /**
     * A Tool representing "no tool", meaning the absence of tool
     */
    public static final Tool NONE = new Tool(false, true);

    /**
     * The Tool corresponding Ink
     */
    private final Ink ink;

    /**
     * The Tool parking position
     */
    private final double parkingX, parkingY;

    /**
     * The Tool loading and unloading instructions
     */
    private final String loadingInstructions, unloadingInstructions;

    /**
     * A flag indicating if this Tool represent an undefined Tool
     */
    private final boolean isUndefinedTool;

    /**
     * A flag indicating if this Tool represent "no Tool"
     */
    private final boolean isNoneTool;

    /**
     * Instantiate a new Tool
     * 
     * @param ink                   the Tool Ink
     * @param parkingX              the Tool X parking position
     * @param parkingY              the Tool Y parking position
     * @param loadingInstructions   the Tool loading instructions
     * @param unloadingInstructions the Tool unloading instructions
     */
    public Tool(Ink ink, double parkingX, double parkingY, String loadingInstructions, String unloadingInstructions) {
        this.ink = ink;
        this.parkingX = parkingX;
        this.parkingY = parkingY;
        this.loadingInstructions = loadingInstructions;
        this.unloadingInstructions = unloadingInstructions;

        this.isUndefinedTool = false;
        this.isNoneTool = false;
    }

    /**
     * Instantiate a new Undefined or None Tool
     * 
     * @param isUndefinedTool true if this Tool represents a Undefined Tool, false otherwise
     * @param isNoneTool      true if this Tool represents a "No Tool", false otherwise
     */
    private Tool(boolean isUndefinedTool, boolean isNoneTool) {
        if (isUndefinedTool && isNoneTool) {
            System.err.println("A Tool should not represent a the same time an Undefined Tool and a No Tool");
        }
        this.isUndefinedTool = isUndefinedTool;
        this.isNoneTool = isNoneTool;

        ink = null;
        parkingX = 0;
        parkingY = 0;
        loadingInstructions = "";
        unloadingInstructions = "";
    }

    /**
     * Get the Tool Ink
     * 
     * @return the Tool Ink
     */
    public Ink getInk() {
        return ink;
    }

    /**
     * Get the Tool X parking position
     * 
     * @return the Tool X parking position
     */
    public double getParkingX() {
        return parkingX;
    }

    /**
     * Get the Tool Y parking position
     * 
     * @return the Tool Y parking position
     */
    public double getParkingY() {
        return parkingY;
    }

    /**
     * Get the Tool loading instructions
     * 
     * @return the Tool loading instructions
     */
    public String getLoadingInstructions() {
        return loadingInstructions;
    }

    /**
     * Get the Tool unloading instructions
     * 
     * @return the Tool unloading instructions
     */
    public String getUnloadingInstructions() {
        return unloadingInstructions;
    }

    /**
     * Indicate if this Tool is an actual tool, i.e. does not represent an Undefined Tool or a "No Tool"
     * 
     * @return true if this Tool is an actual tool, false otherwise
     */
    public boolean isAnActualTool() {
        return !isUndefinedTool && !isNoneTool;
    }

    @Override
    public String toString() {
        if (isUndefinedTool) {
            return "[Tool: Undefined]";
        }
        if (isNoneTool) {
            return "[Tool: None]";
        }
        return String.format(Locale.US, "[Tool: ink:%s at (%.0f,%.0f)]", ink.getName(), parkingX, parkingY);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Tool) {
            Tool other = (Tool) o;
            if (isUndefinedTool) {
                return other.isUndefinedTool;
            }
            if (isNoneTool) {
                return other.isNoneTool;
            }
            return other.ink == ink;
        }
        return false;
    }

    @Override
    public JSONObject toJSonObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("ink", ink.getName());
        jsonObject.put("parking_x", parkingX);
        jsonObject.put("parking_y", parkingY);
        jsonObject.put("loading_instructions", loadingInstructions);
        jsonObject.put("unloading_instructions", unloadingInstructions);
        return jsonObject;
    }

    public static Jsonizable fromJsonObject(JSONObject jsonObject) {
        return new Tool(Ink.getAvailableInkByName(jsonObject.getString("ink")), jsonObject.getDouble("parking_x"),
                jsonObject.getDouble("parking_y"), jsonObject.getString("loading_instructions"),
                jsonObject.getString("unloading_instructions"));
    }

}
