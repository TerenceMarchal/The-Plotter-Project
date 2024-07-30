package streaming.generators;

import java.util.Locale;

import common.engine.Ink;
import common.engine.Utils;
import streaming.engine.Tool;
import streaming.enums.PlotterSetting;
import streaming.session.PlotterConfiguration;

/**
 * A helper used to generate G-Code instructions
 * 
 * TODO-023: try to get rid of this class
 * 
 * @author Terence
 *
 */
public abstract class GcodeGenerator {

    /**
     * The ACK response sent from the Plotter to acknowledge an Instruction
     */
    public final static String ACK_RESPONSE = "ok";

    /**
     * The G-Code query to send to the Plotter to retrieve its status
     */
    public final static String STATUS_QUERY = "?";

    /**
     * The G-Code request to send to the Plotter to retrieve its current configuration
     */
    public final static String READ_CONFIGURATION_REQUEST = "$$";

    /**
     * The Home G-Code instruction
     */
    public final static String HOME_INSTRUCTION = "$H\n";

    /**
     * The Reset G-Code instruction
     * 
     * TODO-013: check it it is the expected reset
     */
    public final static String RESET_INSTRUCTION = new String(new byte[] { 0x18 });

    /**
     * The Unlock G-Code instruction
     */
    public final static String UNLOCK_INSTRUCTION = "$X\n";

    /**
     * The Feed Hold G-Code instruction
     */
    public final static String FEED_HOLD_INSTRUCTION = "!";

    /**
     * The Feed Resume G-Code instruction
     */
    public final static String FEED_RESUME_INSTRUCTION = "~";

    /**
     * The message sent by the Plotter when it is ready to receive instructions
     */
    public final static String PLOTTER_READY_TO_RECEIVE_INSTRUCTIONS_NOTIFICATION = "[MSG:'$H'|'$X' to unlock]";

    /**
     * Generate a fast linear motion G-Code instruction
     * 
     * @param x the destination absolute X coordinate
     * @param y the destination absolute Y coordinate
     * @param z the destination absolute Z coordinate
     * @return the generated G-Code instruction
     */
    public static String fastLinearMovement(double x, double y, double z) {
        String gcode = String.format(Locale.US, "G0 F%.0f ",
                Utils.speedToFeedrate(
                        PlotterConfiguration.Instance.getDoubleSettingValue(PlotterSetting.FLYING_XY_MAX_SPEED),
                        PlotterConfiguration.Instance.getBooleanSettingValue(PlotterSetting.IS_CORE_XY)));
        if (!Double.isNaN(x)) {
            gcode += String.format(Locale.US, "X%.4f ", x);
        }
        if (!Double.isNaN(y)) {
            gcode += String.format(Locale.US, "Y%.4f ", y);
        }
        if (!Double.isNaN(z)) {
            gcode += String.format(Locale.US, "Z%.4f ", z);
        }
        return gcode.trim();
    }

    /**
     * Generate a loaded linear motion G-Code instruction
     * 
     * @param x the destination absolute X coordinate
     * @param y the destination absolute Y coordinate
     * @param z the destination absolute Z coordinate
     * @return the generated G-Code instruction
     */
    public static String loadedLinearMovement(double x, double y, double z) {
        String gcode = String.format(Locale.US, "G1 F%.0f ",
                Utils.speedToFeedrate(
                        PlotterConfiguration.Instance.getDoubleSettingValue(PlotterSetting.DRAWING_XY_MAX_SPEED),
                        PlotterConfiguration.Instance.getBooleanSettingValue(PlotterSetting.IS_CORE_XY)));
        if (!Double.isNaN(x)) {
            gcode += String.format(Locale.US, "X%.4f ", x);
        }
        if (!Double.isNaN(y)) {
            gcode += String.format(Locale.US, "Y%.4f ", y);
        }
        if (!Double.isNaN(z)) {
            gcode += String.format(Locale.US, "Z%.4f ", z);
        }
        return gcode.trim();
    }

    /**
     * Generate a relative fast linear motion G-Code instruction
     * 
     * @param dx the relative motion X coordinate
     * @param dy the relative motion Y coordinate
     * @param dz the relative motion Z coordinate
     * @return the generated G-Code instruction
     */
    public static String relativeFastLinearMovement(double dx, double dy, double dz) {
        double feedrate = Utils.speedToFeedrate(
                PlotterConfiguration.Instance.getDoubleSettingValue(PlotterSetting.FLYING_XY_MAX_SPEED),
                PlotterConfiguration.Instance.getBooleanSettingValue(PlotterSetting.IS_CORE_XY));
        return String.format(Locale.US, "G91\nX%.4f Y%.4f Z%.4f F%.0f\nG90\n", dx, dy, dz, feedrate);
    }

    /**
     * Generate a jog motion G-Code instruction
     * 
     * @param dx       the relative motion X coordinate
     * @param dy       the relative motion Y coordinate
     * @param dz       the relative motion Z coordinate
     * @param feedrate the jog motion feedrate
     * @return the generated G-Code instruction
     */
    public static String jogMotion(double dx, double dy, double dz, double feedrate) {
        return String.format(Locale.US, "$J=G21 G91 X%.4f Y%.4f Z%.4f F%.0f\n", dx, dy, dz, feedrate);
    }

    /**
     * Replace the Tool-change instructions by motions instructions
     * 
     * @param originalInstructions the original set of G-Code instructions
     * @param initialLoadedTool    the initially loaded Tool, before the specified set of instructions are executed
     * @return the original G-Code instructions with the Tool-change instructions replaced
     */
    public static String replaceToolChangeInstructions(String originalInstructions, Tool initialLoadedTool) {
        String parsedInstructions = "";
        Tool currentTool = initialLoadedTool;
        for (String instruction : originalInstructions.split("\n")) {
            if (instruction.startsWith("T")) {
                if (instruction.indexOf(' ') != -1) {
                    instruction = instruction.substring(1, instruction.indexOf(' '));
                } else {
                    instruction = instruction.substring(1);
                }
                int color = Integer.parseInt(instruction);
                Ink ink = Ink.getAvailableInkByColor(color);
                Tool tool = PlotterConfiguration.Instance.getToolByInk(ink);
                if (color >= 0 && (ink == null || tool == Tool.UNDEFINED)) {
                    // TODO-023: add a pause instruction or something?
                    System.out.println(String.format(Locale.US, "Unavailable color: 0x%s (tool id %d)",
                            Integer.toHexString(color), color));
                } else {
                    if (currentTool.isAnActualTool()) {
                        parsedInstructions += ";!nonTranslatable - tool unloading start\n";
                        parsedInstructions += currentTool.getUnloadingInstructions();
                        parsedInstructions += ";!translatable - tool unloading end\n";
                    }
                    if (tool.isAnActualTool()) {
                        parsedInstructions += ";!nonTranslatable - tool loading start\n";
                        parsedInstructions += tool.getLoadingInstructions();
                        parsedInstructions += ";!translatable - tool unloading end\n";
                    }
                    currentTool = tool;
                }
            } else {
                parsedInstructions += instruction + "\n";
            }
        }
        return parsedInstructions;
    }

    /**
     * Get the PlotterSetting corresponding to the GRBL setting ID
     * 
     * @param grblSettingId the GRBL setting ID
     * @return the corresponding PlotterSetting, or null if none corresponds
     */
    public static PlotterSetting getPlotterSettingByGrblId(int grblSettingId) {
        switch (grblSettingId) {
        case 110:
        case 111:
            return PlotterSetting.FLYING_XY_MAX_SPEED;
        case 112:
            return PlotterSetting.Z_MAX_SPEED;
        case 120:
        case 121:
            return PlotterSetting.XY_ACCELERATION;
        case 122:
            return PlotterSetting.Z_ACCELERATION;
        case 130:
            return PlotterSetting.REACHABLE_AREA_WIDTH;
        case 131:
            return PlotterSetting.REACHABLE_AREA_HEIGHT;
        default:
            return null;
        }
    }
}
