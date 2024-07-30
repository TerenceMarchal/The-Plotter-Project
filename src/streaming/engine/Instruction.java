package streaming.engine;

import java.util.Locale;

import common.engine.Ink;
import common.engine.Utils;
import streaming.enums.InstructionType;
import streaming.enums.PlotterSetting;
import streaming.generators.GcodeGenerator;
import streaming.session.PlotterConfiguration;

/**
 * A class representing an Instruction
 * 
 * Note: currently represents only G-Code and specific tool-change instructions
 * 
 * @author Terence
 *
 */
public class Instruction {

    /**
     * The Job to which this Instruction belongs
     */
    private final Job job;

    /**
     * The raw text instruction corresponding to this Instruction
     */
    private final String rawInstruction;

    /**
     * The Instruction type
     * 
     * TODO-023: we should create class inherited from Instruction instead, and make all the properties final
     */
    private InstructionType type;

    /**
     * The Tool to load, if this is a Tool loading Instruction
     */
    private Tool toolToLoad = Tool.UNDEFINED;

    /**
     * The start and end positions of this Instruction
     */
    private double[] startPosition, endPosition;

    /**
     * The motion length of this Instruction
     */
    private double motionLength = 0;

    /**
     * The estimated duration of this Instruction
     */
    private double estimatedDuration = 0;

    /**
     * The timestamp at which this Instruction actually started
     */
    private long startTimestamp;

    /**
     * The timestamp at which this Instruction actually ended
     */
    private long endTimestamp;

    /**
     * Instantiate a new Instruction
     * 
     * @param job            the Job to which the Instruction belong
     * @param rawInstruction raw text instruction from which to generate the Instruction
     * @param startPosition  the start position of the Instruction
     */
    public Instruction(Job job, String rawInstruction, double[] startPosition) {
        this.job = job;
        this.rawInstruction = rawInstruction;
        this.startPosition = startPosition;
        this.endPosition = startPosition.clone();
        parseRawInstruction();
    }

    /**
     * Instantiate a new Instruction, that is not part of a Job
     * 
     * @param rawInstruction raw text instruction from which to generate the Instruction
     */
    public Instruction(String rawInstruction) {
        this.job = null;
        this.rawInstruction = rawInstruction;
        this.startPosition = null;
        this.endPosition = null;
        parseRawInstruction();
    }

    private void parseRawInstruction() {
        String[] params = rawInstruction.split(" ");
        type = parseInstructionType(params[0]);
        if (isMotion()) {
            endPosition = new double[] { startPosition[0], startPosition[1], startPosition[2] };
            for (int idParam = 0; idParam < params.length; idParam++) {
                String param = params[idParam].toUpperCase();
                params[idParam] = param;
                for (int idAxis = 0; idAxis < 3; idAxis++) {
                    if (param.startsWith((char) ('X' + idAxis) + "")) {
                        endPosition[idAxis] = Double.parseDouble(param.substring(1));
                        break;
                    }
                }
            }
            if (isLinearMotion()) {
                motionLength = Math.sqrt(
                        Math.pow(endPosition[0] - startPosition[0], 2) + Math.pow(endPosition[1] - startPosition[1], 2)
                                + Math.pow(endPosition[2] - startPosition[2], 2));
                double speedMaxXY = isFastMotion()
                        ? PlotterConfiguration.Instance.getDoubleSettingValue(PlotterSetting.FLYING_XY_MAX_SPEED)
                        : PlotterConfiguration.Instance.getDoubleSettingValue(PlotterSetting.DRAWING_XY_MAX_SPEED);
                estimatedDuration = Utils.computeXYZTravelDuration(startPosition[0], startPosition[1], startPosition[2],
                        endPosition[0], endPosition[1], endPosition[2],
                        PlotterConfiguration.Instance.getDoubleSettingValue(PlotterSetting.XY_ACCELERATION), speedMaxXY,
                        PlotterConfiguration.Instance.getDoubleSettingValue(PlotterSetting.Z_ACCELERATION),
                        PlotterConfiguration.Instance.getDoubleSettingValue(PlotterSetting.Z_MAX_SPEED),
                        PlotterConfiguration.Instance.getBooleanSettingValue(PlotterSetting.IS_CORE_XY));
            }
        }
    }

    /**
     * Parse the Instruction type from the raw instruction G-Code command code
     * 
     * @param gcodeCommand the raw instruction G-Code command code
     * @return the Instruction type corresponding to the G-Code command code
     */
    private InstructionType parseInstructionType(String gcodeCommand) {
        if (gcodeCommand.equals("G0") || gcodeCommand.equals("G00")) {
            return InstructionType.FAST_LINEAR_MOVEMENT;
        }
        if (gcodeCommand.equals("G1") || gcodeCommand.equals("G01")) {
            return InstructionType.LOADED_LINEAR_MOVEMENT;
        }
        if (gcodeCommand.startsWith("F")) {
            return InstructionType.SET_FEED_RATE;
        }
        if (gcodeCommand.equals("G20")) {
            return InstructionType.USE_INCHES_UNITS;
        }
        if (gcodeCommand.equals("G21")) {
            return InstructionType.USE_MM_UNITS;
        }
        if (gcodeCommand.equals("G90")) {
            return InstructionType.USE_ABSOLUTE_COORDINATES;
        }
        if (gcodeCommand.equals("G91")) {
            return InstructionType.USE_RELATIVE_COORDINATES;
        }
        if (gcodeCommand.startsWith("$J=")) {
            return InstructionType.JOG;
        }
        if (gcodeCommand.equals("$H")) {
            return InstructionType.HOME;
        }
        if (gcodeCommand.equals("$X")) {
            return InstructionType.UNLOCK;
        }
        if (gcodeCommand.equals(new String(new byte[] { 0x18 }))) {
            return InstructionType.RESET;
        }
        if (gcodeCommand.equals("!")) {
            return InstructionType.FEED_HOLD;
        }
        if (gcodeCommand.equals("~")) {
            return InstructionType.FEED_RESUME;
        }
        if (gcodeCommand.equals("$$")) {
            return InstructionType.READ_CONFIGURATION;
        }
        if (gcodeCommand.startsWith(";")) {
            return InstructionType.COMMENT;
        }
        if (gcodeCommand.startsWith("T")) {
            int idTool = Integer.parseInt(gcodeCommand.substring("T".length()));
            if (idTool >= 0) {
                Ink ink = Ink.getAvailableInkByColor(idTool);
                if (ink != null) {
                    toolToLoad = PlotterConfiguration.Instance.getToolByInk(ink);
                } else {
                    System.out
                            .println(String.format(Locale.US, "Unavailable color: 0x%x (tool id: %d)", idTool, idTool));
                }
            } else {
                toolToLoad = Tool.NONE;
            }
            return InstructionType.TOOL_CHANGE;
        }
        if (gcodeCommand.startsWith("G4P0;TOOL:")) {
            int idTool = Integer.parseInt(gcodeCommand.substring("G4P0;TOOL:".length()));
            if (idTool >= 0) {
                Ink ink = Ink.getAvailableInkByColor(idTool);
                if (ink != null) {
                    toolToLoad = PlotterConfiguration.Instance.getToolByInk(ink);
                } else {
                    System.out
                            .println(String.format(Locale.US, "Unavailable color: 0x%x (tool id: %d)", idTool, idTool));
                }
            } else {
                toolToLoad = Tool.NONE;
            }
            return InstructionType.TOOL_CHANGE_NOTIFICATION;
        }
        return InstructionType.UNKNOWN;
    }

    /**
     * Indicate if this Instruction represents a motion instruction
     * 
     * @return true if this Instruction represents a motion instruction, false otherwise
     */
    public boolean isMotion() {
        // TODO-023: update that to also include circular motions
        return isLinearMotion();
    }

    /**
     * Indicate if this Instruction represents a drawing motion instruction
     * 
     * @return true if this Instruction represents a drawing motion instruction, false otherwise
     */
    public boolean isDrawingMotion() { // TODO-023: should we use loaded type instead?
        if (isMotion() && !isZAxisOnlyMotion() && startPosition[2] == 0 && endPosition[2] == 0) {
            if (type != InstructionType.LOADED_LINEAR_MOVEMENT) {
                // TODO-023: also handle non-linear movements here
                // System.out.println("Warning: floor-level motion but of unexpected type:" + type);
                // System.out.println(rawInstruction);
                return false;
            }
            return true;
        } else {
            if (type == InstructionType.LOADED_LINEAR_MOVEMENT) {
                // TODO-023: that is not expected but maybe it happens sometime?
                System.out.println("Warning: loaded movement higher than floor level");
            }
            return false;
        }
    }

    /**
     * Indicate if this Instruction represents a linear motion instruction
     * 
     * @return true if this Instruction represents a linear motion instruction, false otherwise
     */
    public boolean isLinearMotion() {
        return type == InstructionType.FAST_LINEAR_MOVEMENT || type == InstructionType.LOADED_LINEAR_MOVEMENT;
    }

    /**
     * Indicate if this Instruction represents a fast motion instruction
     * 
     * @return true if this Instruction represents a fast motion instruction, false otherwise
     */
    public boolean isFastMotion() {
        return type == InstructionType.FAST_LINEAR_MOVEMENT;
    }

    /**
     * Indicate if this Instruction represents a Z-axis-only motion instruction
     * 
     * @return true if this Instruction represents a Z-axis-only motion instruction, false otherwise
     */
    public boolean isZAxisOnlyMotion() {
        return isMotion() && startPosition[0] == endPosition[0] && startPosition[1] == endPosition[1]
                && startPosition[2] != endPosition[2];
    }

    /**
     * Get the raw text instruction corresponding to this Instruction
     * 
     * TODO-023: this method should not be available anymore
     * 
     * @return the raw text instruction corresponding to this Instruction
     */
    public String getRawInstruction() {
        return rawInstruction;
    }

    /**
     * Get the Instruction String to send to the Plotter
     * 
     * @return the Instruction String to send to the Plotter
     */
    public String getInstructionToStream() {
        if (type == InstructionType.FAST_LINEAR_MOVEMENT || type == InstructionType.LOADED_LINEAR_MOVEMENT) {
            double[] coordinates = new double[] { Double.NaN, Double.NaN, Double.NaN };
            double[] jobOffset = job.getTranslation();
            for (int axis = 0; axis < 3; axis++) {
                // TODO-023: should we do that only if (endPosition[axis] - startPosition[axis] != 0 ||
                // isJobFirstMotionInstruction)?
                coordinates[axis] = endPosition[axis] + (axis != 2 ? jobOffset[axis] : 0);
            }
            return type == InstructionType.FAST_LINEAR_MOVEMENT
                    ? GcodeGenerator.fastLinearMovement(coordinates[0], coordinates[1], coordinates[2])
                    : GcodeGenerator.loadedLinearMovement(coordinates[0], coordinates[1], coordinates[2]);
        }
        return rawInstruction;
    }

    /**
     * Get the Instruction type
     * 
     * @return the Instruction type
     */
    public InstructionType getType() {
        return type;
    }

    /**
     * Get the Instruction Tool to load, if it is a Tool-loading Instruction
     * 
     * @return the Instruction Tool to load if it is a Tool-loading Instruction, null otherwise
     */
    public Tool getToolToLoad() {
        return toolToLoad;
    }

    /**
     * Set the Instruction starting position
     * 
     * TODO-023: could we do something cleaner?
     * 
     * @param startPosition the Instruction starting position
     */
    public void setStartPosition(double[] startPosition) {
        if (this.startPosition == null) {
            this.startPosition = startPosition;
        } else {
            System.out.println("Cannot redefine instruction start position");
        }
    }

    /**
     * Get the Instruction starting position
     * 
     * @return the Instruction starting position
     */
    public double[] getStartPosition() {
        return startPosition;
    }

    /**
     * Get the Instruction end position
     * 
     * @return the Instruction end position
     */
    public double[] getEndPosition() {
        return endPosition;
    }

    /**
     * Get the Instruction motion coordinates, i.e. the coordinates of the start position to end position translation
     * 
     * TODO-023: could we do something cleaner?
     * 
     * @return the Instruction motion coordinates
     */
    public double[] getMotionCoordinates() {
        return new double[] { endPosition[0] - startPosition[0], endPosition[1] - startPosition[1],
                endPosition[2] - startPosition[2] };
    }

    /**
     * Get the Instruction motion length
     * 
     * @return the Instruction motion length
     */
    public double getMotionLength() {
        return motionLength;
    }

    /**
     * Get the Instruction estimated duration
     * 
     * @return the Instruction estimated duration
     */
    public double getEstimatedDuration() {
        return estimatedDuration;
    }

    /**
     * Set the Instruction start timestamp, i.e. the timestamp at which it was sent to the Plotter
     * 
     * TODO-012: it should probably be the timestamp at which it start being executed by the Plotter
     * 
     * @return the Instruction start timestamp
     */
    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    /**
     * Set the Instruction end timestamp, i.e. the timestamp at which it is ACK-ed by the Plotter
     * 
     * @param endTimestamp the Instruction end timestamp
     */
    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    /**
     * Get the Instruction real (i.e. mesured) duration
     * 
     * @return the Instruction real duration
     */
    public int getRealDuration() {
        return (int) (endTimestamp - startTimestamp);
    }

}
