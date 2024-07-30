package streaming.engine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import common.engine.Ink;
import streaming.enums.InstructionType;

/**
 * A class representing a Job, i.e. a set of Instructions
 * 
 * @author Terence
 *
 */
public class Job {

    /**
     * The ID of the current Instruction
     */
    private int currentInstructionId = 0;

    /**
     * The Instructions of the Job
     */
    private final Instruction[] instructions;

    /**
     * Indicate if the job can run even if the loaded Tool is unknown
     */
    private final boolean canRunWithUnknownLoadedTool;

    /**
     * Indicate if the Job is in absolute coordinates, as opposed to relative coordinates
     */
    private boolean isInAbsoluteCoordinates = true;

    /**
     * Indicate if the Job is in inches units, as opposed to mm units
     */
    private boolean isInInchesUnits = false;

    /**
     * An array containing the total drawed length at the end of every Instruction
     */
    private double[] drawedLenghAtInstruction;

    /**
     * An array containing the estimated total duration since startup to reach the end of every Instruction
     */
    private double[] estimatedDurationAtInstruction;

    /**
     * An array containing the real (i.e. mesured) duration since startup to reach the end of every Instruction
     */
    private double[] realDurationAtInstruction;

    /**
     * The timestamp at which the Job started
     */
    private long timestampAtJobStart;

    /**
     * The minimum and maximum positions reached during the Job, i.e. the top-left and bottom-right coordinates of a
     * rectangle containing the Job
     */
    private double[] minPosition = new double[] { 0, 0 }, maxPosition = new double[] { 0, 0 };

    /**
     * The Job translation, applied to every of its Instruction
     */
    private double[] translation = new double[] { 0, 0 };

    /**
     * An ordered HashMap containing the IDs of the first and last Instructions, per Ink
     */
    private LinkedHashMap<Ink, int[]> firstAndLastInstructionsIdsPerInk = new LinkedHashMap<Ink, int[]>();

    /**
     * Instantiate a new Job
     * 
     * @param gcode                       the G-Code from which to retrieve the Instructions of the Job
     * @param initialTool                 the Tool that should be loaded on the Plotter when the first Instruction is
     *                                    executed
     * @param canRunWithUnknownLoadedTool indicate if this job can run even if the loaded Tool is unknown
     */
    public Job(String gcode, Tool initialTool, boolean canRunWithUnknownLoadedTool) {
        // TODO-023: handle corrupted or invalid file
        String[] rawInstructions = gcode.split("\n");
        int nbInstructions = rawInstructions.length;
        instructions = new Instruction[nbInstructions];
        this.canRunWithUnknownLoadedTool = canRunWithUnknownLoadedTool;

        drawedLenghAtInstruction = new double[nbInstructions + 1];
        drawedLenghAtInstruction[0] = 0;
        estimatedDurationAtInstruction = new double[nbInstructions + 1];
        estimatedDurationAtInstruction[0] = 0;
        realDurationAtInstruction = new double[nbInstructions + 1];
        realDurationAtInstruction[0] = 0;
        Tool currentTool = initialTool;
        if (initialTool.isAnActualTool()) {
            firstAndLastInstructionsIdsPerInk.put(initialTool.getInk(), new int[] { 0, nbInstructions });
        }
        double[] position = new double[] { 0, 0, 0 };
        boolean startingPositionFound = false;
        double drawedDistance = 0;
        double estimatedDuration = 0;
        for (int idInstr = 0; idInstr < nbInstructions; idInstr++) {
            Instruction instruction = new Instruction(this, rawInstructions[idInstr], position);
            if (instruction.isMotion() && !instruction.isZAxisOnlyMotion()) {
                if (!startingPositionFound) {
                    minPosition = instruction.getEndPosition().clone();
                    maxPosition = instruction.getEndPosition().clone();
                    startingPositionFound = true;
                    instruction = new Instruction(this, rawInstructions[idInstr], instruction.getEndPosition());
                }
            }
            position = instruction.getEndPosition();
            if (position[0] < minPosition[0]) {
                minPosition[0] = position[0];
            } else if (position[0] > maxPosition[0]) {
                maxPosition[0] = position[0];
            }
            if (position[1] < minPosition[1]) {
                minPosition[1] = position[1];
            } else if (position[1] > maxPosition[1]) {
                maxPosition[1] = position[1];
            }

            instructions[idInstr] = instruction;
            drawedDistance += instruction.isDrawingMotion() ? instruction.getMotionLength() : 0;
            estimatedDuration += instruction.getEstimatedDuration();
            drawedLenghAtInstruction[idInstr + 1] = drawedDistance;
            estimatedDurationAtInstruction[idInstr + 1] = estimatedDuration;
            InstructionType type = instruction.getType();
            if (type == InstructionType.TOOL_CHANGE) {
                if (currentTool.isAnActualTool()) {
                    firstAndLastInstructionsIdsPerInk.get(currentTool.getInk())[1] = idInstr - 1;
                }
                currentTool = instruction.getToolToLoad();
                if (currentTool.isAnActualTool()) {
                    Ink ink = instruction.getToolToLoad().getInk();
                    firstAndLastInstructionsIdsPerInk.put(ink, new int[] { idInstr, nbInstructions });
                }
            }
        }
    }

    /**
     * Indicate if the Job is compatible with the Plotter
     * 
     * @return true if the Job can be run by the Plotter, false otherwise
     */
    public boolean isCompatibleWithPlotter() {
        boolean alreadyReachedMotionInstructions = false;
        boolean coordinatesSystemAlreadySet = false;
        boolean unitsSystemAlreadySet = false;
        for (Instruction instr : instructions) {
            switch (instr.getType()) {
            case FAST_LINEAR_MOVEMENT:
            case LOADED_LINEAR_MOVEMENT:
                alreadyReachedMotionInstructions = true;
                break;
            case USE_INCHES_UNITS:
                isInInchesUnits = true;
                // TODO-027: handle jobs in inches
                System.out.println("Incompatible job: use inches");
                return false;
            case USE_MM_UNITS:
                if (unitsSystemAlreadySet && isInInchesUnits) {
                    System.out.println("Incompatible job: contains multiple units system changes");
                    return false;
                }
                unitsSystemAlreadySet = true;
                break;
            case USE_RELATIVE_COORDINATES:
                isInAbsoluteCoordinates = false;
                // TODO-027: handle jobs relative coordinates
                System.out.println("Incompatible job: use relatives coordinates");
                return false;
            case USE_ABSOLUTE_COORDINATES:
                if (coordinatesSystemAlreadySet && !isInAbsoluteCoordinates) {
                    System.out.println("Incompatible job: contains multiple coordinates system changes");
                    return false;
                }
                coordinatesSystemAlreadySet = true;
                break;
            case UNKNOWN:
                System.out.println("Incompatible job: contains unknown instruction type: " + instr.getRawInstruction());
                return false;
            }
        }
        return true;
    }

    /**
     * Indicate if the Job has been completed, i.e. all its Instructions have been retrieved
     * 
     * @return true if the Job is done, false otherwise
     */
    public boolean isDone() {
        return currentInstructionId >= instructions.length;
    }

    /**
     * Get an HashMap containing the Instructions per Ink
     * 
     * @return an HashMap containing the Instructions per Ink
     */
    public HashMap<Ink, Instruction[]> getInstructionsPerInk() {
        HashMap<Ink, Instruction[]> instructionsPerInk = new HashMap<Ink, Instruction[]>();
        for (Entry<Ink, int[]> entry : firstAndLastInstructionsIdsPerInk.entrySet()) {
            instructionsPerInk.put(entry.getKey(),
                    Arrays.copyOfRange(instructions, entry.getValue()[0], entry.getValue()[1]));
        }
        return instructionsPerInk;
    }

    /**
     * Get the Job current Instruction
     * 
     * @return the Job current Instruction
     */
    public Instruction getCurrentInstruction() {
        if (currentInstructionId == 0) { // TODO-032: do something cleaner?
            timestampAtJobStart = System.currentTimeMillis();
        }
        return instructions[currentInstructionId];
    }

    /**
     * Advance the Job current Instruction to the next one
     */
    public void nextInstruction() {
        currentInstructionId++;
    }

    /**
     * Cancel a Job, i.e. mark it as done
     */
    public void cancelJob() {
        currentInstructionId = instructions.length;
    }

    /**
     * Indicate if this Job can run even if the loaded Tool is unknown
     * 
     * @return true if the Job can be run even if the loaded Tool is unknown, false otherwise
     */
    public boolean canRunWithUnknownLoadedTool() {
        return canRunWithUnknownLoadedTool;
    }

    /**
     * Get the minimum position reached during the Job, i.e. the top-left coordinate of a rectangle containing the Job
     * 
     * @return the minimum position reached during the Job
     */
    public double[] getMinPosition() {
        return minPosition;
    }

    /**
     * Get the minimum position reached during the Job, i.e. the bottom-right coordinate of a rectangle containing the
     * Job
     * 
     * @return the maximum position reached during the Job
     */
    public double[] getMaxPosition() {
        return maxPosition;
    }

    /**
     * Get the width of the Job, i.e. the width of a rectangle containing the Job
     * 
     * @return the width of the Job
     */
    public double getWidth() {
        return maxPosition[0] - minPosition[0];
    }

    /**
     * Get the height of the Job, i.e. the height of a rectangle containing the Job
     * 
     * @return the height of the Job
     */
    public double getHeight() {
        return maxPosition[1] - minPosition[1];
    }

    /**
     * Set the Job translation, applied to all its Instructions
     * 
     * @param translation the translation to apply to the Job
     */
    public void setTranslation(double[] translation) {
        this.translation = translation;
    }

    /**
     * Get the Job translation
     * 
     * @return the translation applied to the Job
     */
    public double[] getTranslation() {
        return translation;
    }

    /**
     * Get the Job translated origin, i.e. the translated top-left coordinate of a rectangle containing the Job
     * 
     * @return the Job translated origin
     */
    public double[] getTranslatedOrigin() {
        return new double[] { minPosition[0] + translation[0], minPosition[1] + translation[1] };
    }

    /**
     * Get the Job drawed distance
     * 
     * @return the Job drawed distance
     */
    public double getDrawedDistance() {
        return drawedLenghAtInstruction[currentInstructionId];
    }

    /**
     * Get the estimated remaining duration for the Job
     * 
     * @return the estimated remaining duration for the Job
     */
    public double getEstimatedRemainingDuration() {
        return estimatedDurationAtInstruction[instructions.length]
                - estimatedDurationAtInstruction[currentInstructionId];
    }

    /**
     * Get the elapsed duration since the Job started, in s
     * 
     * @return the elapsed duration since the Job started, in s
     */
    public long getElapsedDurationSinceJobStart() {
        return (System.currentTimeMillis() - timestampAtJobStart) / 1000;
    }

    /**
     * Get the number of Instructions per Ink
     * 
     * @return an HashMap containing the number of Instructions per Ink
     */
    public HashMap<Ink, Integer> getNbInstructionsPerInk() {
        return getNbExecutedInstructionsPerInkAt(instructions.length);
    }

    /**
     * Get the drawing distances per Ink
     * 
     * @return an HashMap containing the drawing distances per Ink
     */
    public HashMap<Ink, Double> getDrawingDistancesPerInk() {
        return getDrawedDistancesPerInkAt(instructions.length);
    }

    /**
     * Get the estimated duration per Ink
     * 
     * @return an HashMap containing the estimated duration per Ink
     */
    public HashMap<Ink, Integer> getEstimatedDurationPerInk() {
        return getEstimatedRemainingDurationPerInkAt(0);
    }

    /**
     * Get the number of executed Instructions per Ink
     * 
     * @return an HashMap containing the number of executed Instructions per Ink
     */
    public HashMap<Ink, Integer> getNbExecutedInstructionsPerInk() {
        return getNbExecutedInstructionsPerInkAt(currentInstructionId);
    }

    /**
     * Get the drawed distances per Ink
     * 
     * @return an HashMap containing the drawed distances per Ink
     */
    public HashMap<Ink, Double> getDrawedDistancesPerInk() {
        return getDrawedDistancesPerInkAt(currentInstructionId);
    }

    /**
     * Get the estimated remaining duration per Ink
     * 
     * @return an HashMap containing the estimated remaining duration per Ink
     */
    public HashMap<Ink, Integer> getEstimatedRemainingDurationPerInk() {
        return getEstimatedRemainingDurationPerInkAt(currentInstructionId);
    }

    /**
     * Get the Job Inks, orderer by their usage order
     * 
     * @return the Job Inks, orderer by their usage order
     */
    public Set<Ink> getOrderedInks() {
        return firstAndLastInstructionsIdsPerInk.keySet();
    }

    /**
     * Export the expected and real durations per Instruction in a CSV file, for analysis purpose
     */
    public void exportExpectedVsRealDurations() {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter("expectedVsRealDurations.csv"));
            // TODO-012: add x, y length
            bw.write(String.format(Locale.US,
                    "id,instructionType,instruction,estimatedDuration,measuredDuration,measuredOverEstimatedDurationRatio,xDistance,yDistance,zDistance,totalDistance\n"));
            for (int idInstr = 0; idInstr < instructions.length; idInstr++) {
                Instruction instr = instructions[idInstr];
                double estimatedDuration = instr.getEstimatedDuration();
                int measuredDuration = instr.getRealDuration();
                double measuredOverEstimatedDurationRatio = measuredDuration / estimatedDuration;
                double[] motionCoordinates = instr.getMotionCoordinates();
                bw.write(String.format(Locale.US, "%d,%s,%s,%f,%d,%f,%f,%f,%f,%f\n", idInstr, instr.getType().name(),
                        instr.getInstructionToStream(), estimatedDuration, measuredDuration,
                        measuredOverEstimatedDurationRatio, motionCoordinates[0], motionCoordinates[1],
                        motionCoordinates[2], instr.getMotionLength()));
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Get an HashMap containing the number of executed Instructions per Ink at the specified Instruction ID
     * 
     * @param idInstruction the ID of the Instruction at which to retrieve the number of executed Instructions per color
     * @return an HashMap containing the number of executed Instructions per color at the specified Instruction ID
     */
    private HashMap<Ink, Integer> getNbExecutedInstructionsPerInkAt(int idInstruction) {
        HashMap<Ink, Integer> nbExecutedInstructionsPerInk = new HashMap<Ink, Integer>();
        for (Ink ink : firstAndLastInstructionsIdsPerInk.keySet()) {
            int[] firstAndLastInstructionsIds = firstAndLastInstructionsIdsPerInk.get(ink);
            int nbExecutedInstructions = 0;
            if (idInstruction >= firstAndLastInstructionsIds[0]) {
                if (idInstruction < firstAndLastInstructionsIds[1]) {
                    nbExecutedInstructions = idInstruction - firstAndLastInstructionsIds[0];
                } else {
                    nbExecutedInstructions = firstAndLastInstructionsIds[1] - firstAndLastInstructionsIds[0];
                }
            }
            nbExecutedInstructionsPerInk.put(ink, nbExecutedInstructions);
        }
        return nbExecutedInstructionsPerInk;
    }

    /**
     * Get an HashMap containing the drawed distances per Ink at the specified Instruction ID
     * 
     * @param idInstruction the ID of the Instruction at which to retrieve the drawed distances per Ink
     * @return an HashMap containing the drawed distances per Ink at the specified Instruction ID
     */
    private HashMap<Ink, Double> getDrawedDistancesPerInkAt(int idInstruction) {
        HashMap<Ink, Double> drawedDistancesPerInk = new HashMap<Ink, Double>();
        for (Ink ink : firstAndLastInstructionsIdsPerInk.keySet()) {
            int[] firstAndLastInstructionsIds = firstAndLastInstructionsIdsPerInk.get(ink);
            double drawedDistance = 0;
            if (idInstruction >= firstAndLastInstructionsIds[0]) {
                if (idInstruction < firstAndLastInstructionsIds[1]) {
                    drawedDistance = drawedLenghAtInstruction[idInstruction]
                            - drawedLenghAtInstruction[firstAndLastInstructionsIds[0]];
                } else {
                    drawedDistance = drawedLenghAtInstruction[firstAndLastInstructionsIds[1]]
                            - drawedLenghAtInstruction[firstAndLastInstructionsIds[0]];
                }
            }
            drawedDistancesPerInk.put(ink, drawedDistance);
        }
        return drawedDistancesPerInk;
    }

    /**
     * Get an HashMap containing the estimated remaining duration per Ink at the specified Instruction ID
     * 
     * @param idInstruction the ID of the Instruction at which to retrieve the estimated remaining duration per Ink
     * @return an HashMap containing the estimated remaining duration per Ink at the specified Instruction ID
     */
    private HashMap<Ink, Integer> getEstimatedRemainingDurationPerInkAt(int idInstruction) {
        HashMap<Ink, Integer> estimatedRemainingDurationPerInk = new HashMap<Ink, Integer>();
        for (Ink ink : firstAndLastInstructionsIdsPerInk.keySet()) {
            int[] firstAndLastInstructionsIds = firstAndLastInstructionsIdsPerInk.get(ink);
            int estimatedRemainingDuration = (int) (estimatedDurationAtInstruction[firstAndLastInstructionsIds[1]]
                    - estimatedDurationAtInstruction[firstAndLastInstructionsIds[0]]);
            if (idInstruction >= firstAndLastInstructionsIds[0]) {
                if (idInstruction < firstAndLastInstructionsIds[1]) {
                    estimatedRemainingDuration = (int) (estimatedDurationAtInstruction[firstAndLastInstructionsIds[1]]
                            - estimatedDurationAtInstruction[idInstruction]);
                } else {
                    estimatedRemainingDuration = 0;
                }
            }
            estimatedRemainingDurationPerInk.put(ink, estimatedRemainingDuration);
        }
        return estimatedRemainingDurationPerInk;
    }

}
