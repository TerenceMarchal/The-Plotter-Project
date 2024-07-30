package streaming.session;

import java.util.LinkedList;
import java.util.Locale;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fazecast.jSerialComm.SerialPort;

import common.engine.Ink;
import streaming.engine.Instruction;
import streaming.engine.Job;
import streaming.engine.Tool;
import streaming.enums.InstructionType;
import streaming.enums.JobPauseStatus;
import streaming.enums.PlotterSetting;
import streaming.enums.PlotterStatus;
import streaming.generators.GcodeGenerator;
import streaming.listeners.PlotterDataListener;
import streaming.listeners.StreamingProgressionListener;

/**
 * The StreamingManager, that handle all communication with the Plotter, sending Instructions and parsing incoming data
 * and telemetry from the Plotter
 * 
 * @author Terence
 *
 */
public class StreamingManager extends Thread {

    /**
     * The Plotter serial communication baudrate
     */
    private static final int PLOTTER_SERIAL_BAUDRATE = 115200;

    /**
     * The Plotter RX data buffer size, in bytes
     */
    private static final int PLOTTER_RX_BUFFER_SIZE = 128;

    /**
     * Request the Plotter status every x ms
     */
    private static final int REQUEST_STATUS_EVERY_X_MS = 100;

    /**
     * The safe height in mm to go up when cleanly pausing a Job
     */
    private static final int PAUSE_SAFE_HEIGHT = 30;

    /**
     * The Singleton instance
     */
    public static final StreamingManager Instance;

    /**
     * The SerialPort used for the communications with the Plotter
     */
    private SerialPort serialPort;

    /**
     * A flag indicating if we are instead "connected" to a Virtual Plotter, that ACK all received Instructions
     */
    private boolean isConnectedToVirtualPlotter = false;

    /**
     * The currently streaming Job
     */
    private Job streamingJob = null;

    /**
     * A list of completed Jobs
     * 
     * TODO-044: also includes the drawed Jobs from previous sessions
     */
    private Vector<Job> completedJobs = new Vector<Job>();

    /**
     * The current Plotter status
     * 
     * Note: it should be set only through firePlotterStatusChanged
     */
    private PlotterStatus plotterStatus = PlotterStatus.DISCONNECTED;

    /**
     * The currenlty loaded Tool on the Plotter
     */
    private Tool loadedTool = Tool.UNDEFINED;

    /**
     * A buffer containing priority Instructions to send to the Plotter, such as the Tool-change Instructions and
     * pause/stop-related Instructions
     */
    private LinkedList<Instruction> priorityInstructionsBuffer = new LinkedList<Instruction>();

    /**
     * The current streaming Job pause status
     */
    private JobPauseStatus streamingJobPauseStatus = JobPauseStatus.ONGOING;

    /**
     * The Plotter Data Listeners
     */
    private Vector<PlotterDataListener> plotterDataListeners = new Vector<PlotterDataListener>();

    /**
     * The Streaming Progression Listeners
     */
    private Vector<StreamingProgressionListener> streamingProgressionListeners = new Vector<StreamingProgressionListener>();

    /**
     * The Plotter latest Work Coordinate Offset (WCO)
     */
    private double[] workCoordinateOffset = new double[3];

    /**
     * Instantiate the Singleton
     */
    static {
        Instance = new StreamingManager();
    }

    /**
     * Instantiate a new StreamingManager
     */
    private StreamingManager() {
        start();
    }

    /**
     * The thread main loop
     */
    public void run() {
        String rxBuffer = "";
        LinkedList<Instruction> sentButNotAcknowledgedInstructions = new LinkedList<Instruction>();
        int dataInPlotterRxBuffer = 0;
        long lastStatusRequestSentAt = 0;

        while (true) {
            if (serialPort != null || isConnectedToVirtualPlotter) {
                /* Read RX data */
                int nbBytesAvailable = !isConnectedToVirtualPlotter ? serialPort.bytesAvailable() : 0;
                if (nbBytesAvailable > 0) {
                    byte[] dataRead = new byte[nbBytesAvailable];
                    if (serialPort.readBytes(dataRead, nbBytesAvailable) != nbBytesAvailable) {
                        System.out.println("Failed to read all available data");
                    }
                    rxBuffer += new String(dataRead);
                }

                /* Parse RX data */
                if (rxBuffer.length() > 0) {
                    int nlPos = rxBuffer.indexOf("\r\n");
                    if (nlPos > -1) {
                        String line = rxBuffer.substring(0, nlPos);
                        rxBuffer = rxBuffer.substring(nlPos + "\r\n".length());
                        if (line.equals(GcodeGenerator.ACK_RESPONSE)) {
                            Instruction acknowledgedInstr = sentButNotAcknowledgedInstructions.poll();
                            if (acknowledgedInstr != null) {
                                acknowledgedInstr.setEndTimestamp(System.currentTimeMillis());
                                // TODO-023: handle the \n properly
                                dataInPlotterRxBuffer -= (acknowledgedInstr.getInstructionToStream() + "\n").length();
                                // TODO-023: use instruction type instead of this startsWith
                                if (acknowledgedInstr.getRawInstruction().startsWith("G4P0;TOOL:")) {
                                    int idTool = Integer.parseInt(acknowledgedInstr.getRawInstruction()
                                            .substring("G4P0;TOOL:".length()).trim());
                                    if (idTool == -1) {
                                        loadedTool = Tool.NONE;
                                    } else {
                                        loadedTool = PlotterConfiguration.Instance
                                                .getToolByInk(Ink.getAvailableInkByColor(idTool));
                                    }
                                    fireLoadedToolChanged(loadedTool);
                                } else if (acknowledgedInstr.getRawInstruction().endsWith(";safe pause\n")) {
                                    // TODO-056: handle clean pause properly
//                                    try {
//                                        Thread.sleep(1000);
//                                    } catch (InterruptedException e) {
//                                        e.printStackTrace();
//                                    }
//                                    priorityInstructionsBuffer
//                                            .add(new Instruction(GcodeGenerator.FEED_HOLD_INSTRUCTION));
                                }
                            }
                            // fireStreamingProgressionListeners(streamingJob); // TODO-012: should it be called from
                            // here?
                            if (streamingJob != null) {
                                if (dataInPlotterRxBuffer == 0 && streamingJob.isDone()) {
                                    completedJobs.add(streamingJob);
                                    // TODO-012: export expected VS real durations .csv file
                                    streamingJob.exportExpectedVsRealDurations();
                                    fireJobCompleted();
                                    streamingJob = null;
                                } else {
                                    if (streamingJob.isDone()) {
                                        System.out.println("job done but data remaining in rxBuffer: " + rxBuffer);
                                    }
                                }
                            }
                        } else if (line.equals(GcodeGenerator.PLOTTER_READY_TO_RECEIVE_INSTRUCTIONS_NOTIFICATION)) {
                            readConfiguration();
                        } else if (line.length() > 0) {
                            Instruction instructionSent = sentButNotAcknowledgedInstructions.peek();
                            parseRxResponse(line,
                                    instructionSent != null ? instructionSent.getInstructionToStream() : "");

                        }
                    }
                }

                /* Send next job instructions */
                if ((streamingJob != null && !streamingJob.isDone()) || !priorityInstructionsBuffer.isEmpty()) {
                    boolean instructionFromPriorityBuffer = false;
                    Instruction instruction;
                    if (!priorityInstructionsBuffer.isEmpty()) {
                        instruction = priorityInstructionsBuffer.peek();
                        instructionFromPriorityBuffer = true;
                    } else {
                        instruction = streamingJob.getCurrentInstruction();
                    }
                    int instrLength = instruction.getInstructionToStream().length() + 1;
                    if (dataInPlotterRxBuffer + instrLength <= PLOTTER_RX_BUFFER_SIZE) {
                        if (instructionFromPriorityBuffer) {
                            priorityInstructionsBuffer.poll();
                        } else {
                            streamingJob.nextInstruction();
                        }
                        if (instruction.getType() == InstructionType.TOOL_CHANGE) {
                            addToolChangeInstructionsToBuffer(instruction.getToolToLoad());
                        } else {
                            sentButNotAcknowledgedInstructions.add(instruction);
                            dataInPlotterRxBuffer += instrLength;
                            instruction.setStartTimestamp(System.currentTimeMillis());
                            if (!isConnectedToVirtualPlotter) {
                                byte[] binaryInstruction = (instruction.getInstructionToStream() + "\n").getBytes();
                                if (binaryInstruction.length != instrLength) {
                                    System.err.println("error: binaryInstruction.length!=instrLength");
                                }
                                if (instructionFromPriorityBuffer) {
                                    System.out.println("priority: " + instruction.getInstructionToStream());
                                }
                                serialPort.writeBytes(binaryInstruction, instrLength);
                            } else {
                                rxBuffer += "ok\r\n";
                            }
                            fireStreamingProgressionListeners(streamingJob);
                        }
                    }

                }

                /* Request status if needed */
                long T = System.currentTimeMillis();
                if (T - lastStatusRequestSentAt >= REQUEST_STATUS_EVERY_X_MS) {
                    if (!isConnectedToVirtualPlotter) {
                        // TODO-023: we should not call the GcodeGenerator like that
                        byte[] statusRequest = GcodeGenerator.STATUS_QUERY.getBytes();
                        serialPort.writeBytes(statusRequest, statusRequest.length);
                        lastStatusRequestSentAt = T;
                    } else {
                        if (streamingJob != null && !streamingJob.isDone()) {
                            double[] workPosition = streamingJob.getCurrentInstruction().getEndPosition().clone();
                            workPosition[0] += streamingJob.getTranslation()[0];
                            workPosition[1] += streamingJob.getTranslation()[1];
                            firePlotterDataChanged(workPosition, new double[] { 0, 0, 0 }, 0,
                                    new boolean[] { false, false, false });
                        }
                    }
                }
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Parse a response received from the Plotter
     * 
     * @param response        the response received from the Plotter
     * @param instructionSent the instruction sent to the Plotter for which we received the response
     */
    private void parseRxResponse(String response, String instructionSent) {
        if (response.charAt(0) == '<') {
            /* Parse status response */
            /* Note: see https://github.com/gnea/grbl/wiki/Grbl-v1.1-Interface#real-time-status-reports */
            response = response.substring(1, response.length() - 1);
            String[] fields = response.split("\\|");
            PlotterStatus plotterStatus = PlotterStatus.DISCONNECTED;
            double[] workCoordinateOffset = null;
            double[] machinePosition = null;
            double[] workPosition = null;
            double currentFeedrate = 0;
            boolean[] endstopsTriggered = new boolean[] { false, false, false };
            String machineState = fields[0];
            if (machineState.contains(":")) {
                machineState = machineState.substring(0, machineState.indexOf(":"));
            }
            switch (machineState) {
            case "Idle":
                plotterStatus = PlotterStatus.IDLE;
                break;
            case "Run":
                plotterStatus = PlotterStatus.RUN;
                break;
            case "Hold":
                plotterStatus = PlotterStatus.HOLD;
                break;
            case "Jog":
                plotterStatus = PlotterStatus.JOG;
                break;
            case "Alarm":
                plotterStatus = PlotterStatus.ALARM;
                break;
            case "Door":
                plotterStatus = PlotterStatus.DOOR;
                break;
            case "Check":
                plotterStatus = PlotterStatus.CHECK;
                break;
            case "Home":
                plotterStatus = PlotterStatus.HOME;
                break;
            case "Sleep":
                plotterStatus = PlotterStatus.SLEEP;
                break;
            }
            for (int idField = 1; idField < fields.length; idField++) {
                String[] keyValue = fields[idField].split(":");
                String[] values = keyValue[1].split(",");
                switch (keyValue[0]) {
                case "MPos":
                    machinePosition = new double[] { Double.parseDouble(values[0]), Double.parseDouble(values[1]),
                            Double.parseDouble(values[2]) };
                    break;
                case "WPos":
                    workPosition = new double[] { Double.parseDouble(values[0]), Double.parseDouble(values[1]),
                            Double.parseDouble(values[2]) };
                    break;
                case "WCO":
                    workCoordinateOffset = new double[] { Double.parseDouble(values[0]), Double.parseDouble(values[1]),
                            Double.parseDouble(values[2]) };
                    break;
                case "F":
                case "FS":
                    currentFeedrate = Double.parseDouble(values[0]);
                    break;
                case "Pn":
                    for (char axis = 0; axis < 3; axis++) {
                        endstopsTriggered[axis] = keyValue[1].contains((char) ('X' + axis) + "");
                    }
                    break;
                }
            }
            if (workCoordinateOffset != null) {
                this.workCoordinateOffset = workCoordinateOffset;
            }
            if (workPosition == null) {
                workPosition = new double[] { machinePosition[0] - this.workCoordinateOffset[0],
                        machinePosition[1] - this.workCoordinateOffset[1],
                        machinePosition[2] - this.workCoordinateOffset[2] };
            } else if (machinePosition == null) {
                machinePosition = new double[] { workPosition[0] + this.workCoordinateOffset[0],
                        workPosition[1] + this.workCoordinateOffset[1],
                        workPosition[2] + this.workCoordinateOffset[2] };
            }
            if (this.plotterStatus != plotterStatus) {
                firePlotterStatusChanged(plotterStatus);
            }
            firePlotterDataChanged(workPosition, machinePosition, currentFeedrate, endstopsTriggered);
        } else if (instructionSent.equals(GcodeGenerator.READ_CONFIGURATION_REQUEST)) {
            /* Parse Plotter current configuration */
            Matcher m = Pattern.compile("\\$(\\d+)\\s*=(\\d*.?\\d+)").matcher(response);
            if (m.find()) {
                int settingId = Integer.parseInt(m.group(1));
                double settingValue = Double.parseDouble(m.group(2));
                PlotterSetting setting = GcodeGenerator.getPlotterSettingByGrblId(settingId);
                if (setting != null) {
                    if (setting == PlotterSetting.FLYING_XY_MAX_SPEED || setting == PlotterSetting.Z_MAX_SPEED) {
                        /* Convert speed setting from mm/min to mm/s */
                        /* TODO-023: better handle the settings units conversions */
                        settingValue /= 60;
                    }
                    PlotterConfiguration.Instance.overrideDoubleSettingValue(setting, settingValue);
                }
            }
        } else if (response.startsWith("error")) {
            /* Parse error response */
            System.out.println(String.format(Locale.US, "Error for instruction %s: %s", instructionSent, response));
        }
    }

    /**
     * Add the Tool-change Instructions needed to load the specified Tool in the ToolChangeInstructionsBuffer
     * 
     * @param tool the Tool for which to generate the loading Instructions
     */
    private void addToolChangeInstructionsToBuffer(Tool tool) {
        if (tool == Tool.UNDEFINED) {
            // TODO-020: let the user select to tool here?
            System.out.println("Undefined tool");
        } else if (tool != loadedTool) {
            String gcode = "";
            if (loadedTool.isAnActualTool()) {
                gcode += loadedTool.getUnloadingInstructions();
            }
            if (tool.isAnActualTool()) {
                gcode += tool.getLoadingInstructions();
            }
            String[] instructions = gcode.split("\n");
            for (int idInstr = 0; idInstr < instructions.length; idInstr++) {
                // TODO-020: do something cleaner
                priorityInstructionsBuffer.add(new Instruction(new Job("", loadedTool, false), instructions[idInstr],
                        new double[] { Double.NaN, Double.NaN, Double.NaN }));
            }
        }
    }

    /**
     * Get the Plotter status
     * 
     * @return the Plotter status
     */
    public PlotterStatus getPlotterStatus() {
        return plotterStatus;
    }

    /**
     * Notify the Plotter Data Listeners that the Plotter status changed
     * 
     * @param status the new Plotter status
     */
    private void firePlotterStatusChanged(PlotterStatus status) {
        plotterStatus = status;
        for (PlotterDataListener listener : plotterDataListeners) {
            listener.plotterStatusChanged(status);
        }
    }

    /**
     * Notify the Plotter Data Listeners that the loaded Tool changed
     * 
     * @param newlyLoadedTool the newly loaded Tool
     */
    private void fireLoadedToolChanged(Tool newlyLoadedTool) {
        for (PlotterDataListener listener : plotterDataListeners) {
            listener.loadedToolChanged(newlyLoadedTool);
        }
    }

    /**
     * Notify the Plotter Data Listeners that the Plotter data changed
     * 
     * @param workPosition      the Plotter current working position
     * @param machinePosition   the Plotter current machine Position
     * @param currentFeedrate   the Plotter current feedrate
     * @param endstopsTriggered an array indicating if the Plotter X, Y and Z endstops are currently triggered
     */
    private void firePlotterDataChanged(double[] workPosition, double[] machinePosition, double currentFeedrate,
            boolean[] endstopsTriggered) {
        for (PlotterDataListener listener : plotterDataListeners) {
            listener.plotterDataChanged(workPosition, machinePosition, currentFeedrate, endstopsTriggered);
        }
    }

    /**
     * Notify the Streaming Progression Listeners that a new Job started
     * 
     * @param job the Job that just started
     */
    private void fireStartedNewJob(Job job) {
        for (StreamingProgressionListener listener : streamingProgressionListeners) {
            listener.startedNewJob(job);
        }
    }

    /**
     * Notify the Streaming Progression Listeners that the current Job progression changed
     * 
     * @param job the current streaming Job
     */
    private void fireStreamingProgressionListeners(Job job) {
        for (StreamingProgressionListener listener : streamingProgressionListeners) {
            listener.streamingProgressionChanged(job.getElapsedDurationSinceJobStart(),
                    job.getNbExecutedInstructionsPerInk(), job.getDrawedDistancesPerInk(),
                    job.getEstimatedRemainingDurationPerInk());
        }
    }

    /**
     * Notify the Streaming Progression Listeners that the currently streaming Job just finished
     * 
     * @param job the Job that just finished
     */
    private void fireJobCompleted() {
        for (StreamingProgressionListener listener : streamingProgressionListeners) {
            listener.jobCompleted();
        }
    }

    /**
     * Get the names of the ports available to stream Instructions to the Plotter
     * 
     * Note: this function currently return only Serial COM ports, but other streaming ports could be handled later
     * 
     * @return the names of available streaming ports
     */
    public String[] getAvailableStreamingPorts() {
        SerialPort[] serialPorts = SerialPort.getCommPorts();
        String[] portNames = new String[serialPorts.length];
        for (int idPort = 0; idPort < serialPorts.length; idPort++) {
            portNames[idPort] = serialPorts[idPort].getSystemPortName();
        }
        return portNames;
    }

    /**
     * Connect the software to the Plotter through the specified streaming port
     * 
     * @param port the port to which to connect to the Plotter
     * @return true if the connection was established successfully, false otherwise
     */
    public boolean connectTo(String port) {
        if (serialPort == null && !isConnectedToVirtualPlotter) {
            SerialPort serialPort = SerialPort.getCommPort(port);
            serialPort.setBaudRate(PLOTTER_SERIAL_BAUDRATE);
            if (serialPort.openPort()) {
                System.out.println("Connected to " + port + " successfully");
                this.serialPort = serialPort;
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * "Connect" the software to a Virtual Plotter that ACK all received Instructions
     * 
     * @return true if the software is now "connected" to the Virtual Plotter, false otherwise
     */
    public boolean connectToVirtualPlotter() {
        if (serialPort != null) {
            return false;
        }
        System.out.println("Connected to Virtual Plotter successfully");
        isConnectedToVirtualPlotter = true;
        firePlotterStatusChanged(PlotterStatus.SIMULATING);
        return true;
    }

    /**
     * Disconnect the software from the Plotter
     * 
     * @return true if the Plotter is now disconnected, false otherwise
     */
    public boolean disconnect() {
        if (serialPort != null) {
            if (serialPort.closePort()) {
                serialPort = null;
                return true;
            } else {
                return false;
            }
        }
        isConnectedToVirtualPlotter = false;
        firePlotterStatusChanged(PlotterStatus.DISCONNECTED);
        return true;
    }

    /**
     * Set the Tool initially loaded on the Plotter
     * 
     * Note: this needs to be called before trying to streaming any Job
     * 
     * @param tool the initially loaded Tool
     */
    public void setInitiallyLoadedTool(Tool tool) {
        if (loadedTool != Tool.UNDEFINED) {
            System.out.println("The loaded tool is known (%s), you must start a tool change job");
        } else {
            loadedTool = tool;
            fireLoadedToolChanged(tool);
        }
    }

    /**
     * Start a tool-change Job
     * 
     * @param tool the Tool to load
     */
    public void startChangeToolJob(Tool tool) {
        if (loadedTool != Tool.UNDEFINED) {
            if (tool != loadedTool) {
                String instructions = "";
                if (loadedTool.isAnActualTool()) {
                    instructions += loadedTool.getUnloadingInstructions();
                }
                if (tool.isAnActualTool()) {
                    instructions += tool.getLoadingInstructions();
                }
                System.out.println("job tool change:");
                System.out.println(instructions);
                startJob(new Job(instructions, loadedTool, false));
            }
        } else {
            /* do nothing, we don't know what tool is (or is not) currently loaded */
            System.out.println("Cannot change tool until initially loaded tool has been defined");
        }
    }

    /**
     * Start the specified Job
     * 
     * @param job the Job to start
     */
    public void startJob(Job job) {
        if (job.isCompatibleWithPlotter()) {
            if (loadedTool != Tool.UNDEFINED || job.canRunWithUnknownLoadedTool()) {
                if (streamingJob == null) {
                    if (serialPort != null || isConnectedToVirtualPlotter) {
                        fireStartedNewJob(job);
                        streamingJob = job;
                    }
                } else {
                    System.out.println("Cannot start multiple jobs at the same time");
                }
            } else {
                System.out.println("Cannot start this kind of job until the currently loaded tool has been defined");
            }
        } else {
            System.out.println("Job incompatible with plotter");
        }
    }

    /**
     * Start a jog motion Job
     * 
     * @param dx       the relative distance to travel on the X axis
     * @param dy       the relative distance to travel on the Y axis
     * @param dz       the relative distance to travel on the Z axis
     * @param feedrate the feedrate with which to perform the jog
     */
    public void jog(double dx, double dy, double dz, double feedrate) {
        startJob(new Job(GcodeGenerator.jogMotion(dx, dy, dz, feedrate), loadedTool, true));
    }

    /**
     * Send a request to read the current Plotter configuration
     */
    public void readConfiguration() {
        startJob(new Job(GcodeGenerator.READ_CONFIGURATION_REQUEST, loadedTool, true));
    }

    /**
     * Home the Plotter
     */
    public void home() {
        startJob(new Job(GcodeGenerator.HOME_INSTRUCTION, loadedTool, true));
    }

    /**
     * Unlock the Plotter
     */
    public void unlock() {
        startJob(new Job(GcodeGenerator.UNLOCK_INSTRUCTION, loadedTool, true));
    }

    /**
     * Reset (i.e. restart) the Plotter
     */
    public void reset() {
        startJob(new Job(GcodeGenerator.RESET_INSTRUCTION, loadedTool, true));
    }

    /**
     * Park the Plotter, i.e. move its head to the parking position
     */
    public void park() {
        startJob(new Job(
                GcodeGenerator.fastLinearMovement(
                        PlotterConfiguration.Instance.getDoubleSettingValue(PlotterSetting.PARKING_X),
                        PlotterConfiguration.Instance.getDoubleSettingValue(PlotterSetting.PARKING_Y),
                        PlotterConfiguration.Instance.getDoubleSettingValue(PlotterSetting.PARKING_Z)),
                loadedTool, true));
    }

    /**
     * Cleanly pause the current Job
     */
    public void pauseJob() {
        // TODO-056: handle clean pause properly
        if (streamingJobPauseStatus == JobPauseStatus.ONGOING) {
            System.out.println("not implemented yet");
//            String safePauseMotion = GcodeGenerator.relativeFastLinearMovement(0, 0, PAUSE_SAFE_HEIGHT);
//            safePauseMotion = safePauseMotion.substring(0, safePauseMotion.length() - 1) + ";safe pause\n";
//            priorityInstructionsBuffer.add(new Instruction(safePauseMotion));
//            priorityInstructionsBuffer.add(new Instruction("G4 P1500\n"));
//            streamingJobPauseStatus = JobPauseStatus.CLEANLY_PAUSED;
        }
    }

    /**
     * Do an emergency pause on the Plotter
     */
    public void emergencyPause() {
        if (streamingJobPauseStatus == JobPauseStatus.ONGOING) {
            priorityInstructionsBuffer.add(new Instruction(GcodeGenerator.FEED_HOLD_INSTRUCTION));
            streamingJobPauseStatus = JobPauseStatus.EMERGENCY_PAUSED;
        }
    }

    /**
     * Resume the current Job after it was paused
     */
    public void resumeJob() {
        if (streamingJobPauseStatus == JobPauseStatus.CLEANLY_PAUSED) {
            // TODO-056: handle clean pause properly
//            priorityInstructionsBuffer.add(new Instruction(GcodeGenerator.FEED_RESUME_INSTRUCTION));
//            priorityInstructionsBuffer
//                    .add(new Instruction(GcodeGenerator.relativeFastLinearMovement(0, 0, -PAUSE_SAFE_HEIGHT)));
        } else if (streamingJobPauseStatus == JobPauseStatus.EMERGENCY_PAUSED) {
            priorityInstructionsBuffer.add(new Instruction(GcodeGenerator.FEED_RESUME_INSTRUCTION));
        }
        streamingJobPauseStatus = JobPauseStatus.ONGOING;
    }

    /**
     * Stop (i.e. abort) the current Job
     */
    public void stopJob() {
        streamingJobPauseStatus = JobPauseStatus.ONGOING;
        streamingJob.cancelJob();
        priorityInstructionsBuffer
                .add(new Instruction(GcodeGenerator.relativeFastLinearMovement(0, 0, PAUSE_SAFE_HEIGHT)));
    }

    /**
     * Get the currently streaming Job
     * 
     * @return the currently streaming Job
     */
    public Job getStreamingJob() {
        return streamingJob;
    }

    /**
     * Get the list of the completed Jobs
     * 
     * @return the list of the completed Jobs
     */
    public Vector<Job> getCompletedJobs() {
        return completedJobs;
    }

    /**
     * Clear the list of the completed Jobs
     */
    public void clearCompletedJobs() {
        completedJobs.clear();
    }

    /**
     * Get the currently loaded Tool on the Plotter
     * 
     * TODO-020: this getter is probably not necessary?
     * 
     * @return the currently loaded Tool on the Plotter
     */
    public Tool getLoadedTool() {
        return loadedTool;
    }

    /**
     * Add a Plotter Data Listener
     * 
     * @param listener the listener to add
     */
    public void addPlotterDataListener(PlotterDataListener listener) {
        plotterDataListeners.add(listener);
    }

    /**
     * Add a Streaming Progression Listener
     * 
     * @param listener the listener to add
     */
    public void addStreamingProgressionListener(StreamingProgressionListener listener) {
        streamingProgressionListeners.add(listener);
    }

}
