package streaming.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import common.engine.Ink;
import common.engine.Utils;
import common.gui.View;
import streaming.engine.Instruction;
import streaming.engine.Job;
import streaming.engine.Tool;
import streaming.enums.PlotterSetting;
import streaming.enums.PlotterStatus;
import streaming.listeners.PlotterDataListener;
import streaming.listeners.StreamingProgressionListener;
import streaming.session.PlotterConfiguration;
import streaming.session.StreamingManager;

/**
 * A View to display the Plotter movements and drawings
 * 
 * @author Terence
 *
 */
public class PlotterView extends View
        implements PlotterDataListener, StreamingProgressionListener, MouseListener, ActionListener {

    /**
     * Indicate if we should show the flying motions (i.e. non-loaded fast movements higher than 0)
     */
    private static final boolean SHOW_FLYING_MOTIONS = true;

    /**
     * The View display resolution
     */
    private static final int PX_PER_MM = 100;

    /**
     * The Plotter drawing area dimensions
     */
    private static final double DRAWING_AREA_WIDTH = PlotterConfiguration.Instance
            .getDoubleSettingValue(PlotterSetting.DRAWING_AREA_WIDTH);
    private static final double DRAWING_AREA_HEIGHT = PlotterConfiguration.Instance
            .getDoubleSettingValue(PlotterSetting.DRAWING_AREA_HEIGHT);

    /**
     * The drawing area offset within the reachable area
     */
    private static final double DRAWING_AREA_X = PlotterConfiguration.Instance
            .getDoubleSettingValue(PlotterSetting.DRAWING_AREA_X);
    private static final double DRAWING_AREA_Y = PlotterConfiguration.Instance
            .getDoubleSettingValue(PlotterSetting.DRAWING_AREA_Y);

    /**
     * The tool width in mm
     */
    private static final double TOOL_WIDTH_IN_MM = 12;

    /**
     * The pen tip width in mm
     */
    private static final double PEN_TIP_WIDTH_IN_MM = 0.25;

    /**
     * The Z height in mm at which movements are considered safe
     */
    private static final double SAFE_Z_HEIGHT = 4;

    /**
     * The color to use to draw the flying motions
     */
    private static final Color FLYING_MOTIONS_COLOR = new Color(255, 255, 0, 128);

    /**
     * The Stroke used to draw pen paths
     */
    private static final Stroke PEN_STROKE = Utils.getPenStrokeInPx(PEN_TIP_WIDTH_IN_MM * PX_PER_MM);

    /**
     * The Stroke used to draw flying paths
     */
    private static final Stroke FLYING_STROKE = Utils.getPenStrokeInPx(0.1 * PX_PER_MM);

    /**
     * The Job currently being previewed
     */
    private Job previewingJob = null;

    /**
     * The currently loaded Tool on the Plotter
     */
    private Tool loadedTool = Tool.UNDEFINED;

    /**
     * The Plotter current work position
     */
    private double[] workPosition;

    /**
     * Indicate if the previewing Job is being translated
     */
    private boolean translatingPreviewingJob = false;

    /**
     * The number of instructions that have been executed per Ink for the streaming Job
     */
    private HashMap<Ink, Integer> nbInstructionsExecutedPerInkForStreamingJob;

    /**
     * The popup menu displayed on a right-clic on the View
     */
    private final JPopupMenu popupMenu;

    /**
     * The menu items of the popup menu
     */
    private JMenuItem moveHereMenuItem, clearDrawingAreaMenuItem, bottomLeftDrawingAreaOriginMenuItem,
            topLeftDrawingAreaOriginMenuItem, bottomRightDrawingAreaOriginMenuItem, topRightDrawingAreaOriginMenuItem,
            parkingPositionMenuItem, tool0ParkingPositionMenuItem;

    /**
     * Instantiate a new Plotter View
     */
    public PlotterView() {
        super("Plotter view");
        viewPanel.addMouseListener(this);
        StreamingManager.Instance.addPlotterDataListener(this);
        StreamingManager.Instance.addStreamingProgressionListener(this);
        popupMenu = initPopupMenu();
    }

    /**
     * Init the View popup menu
     * 
     * @return the initialized popup menu
     */
    private JPopupMenu initPopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();

        /* Move here */
        moveHereMenuItem = new JMenuItem("Move here");
        moveHereMenuItem.addActionListener(this);
        popupMenu.add(moveHereMenuItem);

        /* Clear drawing area */
        clearDrawingAreaMenuItem = new JMenuItem("Clear drawing area");
        clearDrawingAreaMenuItem.addActionListener(this);
        popupMenu.add(clearDrawingAreaMenuItem);
        popupMenu.addSeparator();

        /* Set drawing area origin */
        JMenu drawingAreaOriginMenu = new JMenu("Set drawing area origin");
        bottomLeftDrawingAreaOriginMenuItem = new JMenuItem("Place bottom-left corner here");
        bottomLeftDrawingAreaOriginMenuItem.addActionListener(this);
        drawingAreaOriginMenu.add(bottomLeftDrawingAreaOriginMenuItem);
        topLeftDrawingAreaOriginMenuItem = new JMenuItem("Place top-left corner here");
        topLeftDrawingAreaOriginMenuItem.addActionListener(this);
        drawingAreaOriginMenu.add(topLeftDrawingAreaOriginMenuItem);
        bottomRightDrawingAreaOriginMenuItem = new JMenuItem("Place bottom-right corner here");
        bottomRightDrawingAreaOriginMenuItem.addActionListener(this);
        drawingAreaOriginMenu.add(bottomRightDrawingAreaOriginMenuItem);
        topRightDrawingAreaOriginMenuItem = new JMenuItem("Place top-right corner here");
        topRightDrawingAreaOriginMenuItem.addActionListener(this);
        drawingAreaOriginMenu.add(topRightDrawingAreaOriginMenuItem);
        popupMenu.add(drawingAreaOriginMenu);

        /* Set parking position */
        parkingPositionMenuItem = new JMenuItem("Set parking position at head position");
        parkingPositionMenuItem.addActionListener(this);
        popupMenu.add(parkingPositionMenuItem);

        /* Set tool 0 parking position */
        tool0ParkingPositionMenuItem = new JMenuItem("Set tool 0 parking position at head position");
        tool0ParkingPositionMenuItem.addActionListener(this);
        popupMenu.add(tool0ParkingPositionMenuItem);

        return popupMenu;
    }

    @Override
    protected void paint(Graphics2D g, int W, int H) {
        double reachableAreaW = PlotterConfiguration.Instance
                .getDoubleSettingValue(PlotterSetting.REACHABLE_AREA_WIDTH);
        double reachableAreaH = PlotterConfiguration.Instance
                .getDoubleSettingValue(PlotterSetting.REACHABLE_AREA_HEIGHT);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(g.getFont().deriveFont(5f * PX_PER_MM));
        if (!zoom.hasBeenInitialized()) {
            double xRatio = (W * 0.95) / (reachableAreaW * PX_PER_MM);
            double yRatio = (H * 0.95) / (reachableAreaH * PX_PER_MM);
            double zoomLevel = Math.min(xRatio, yRatio);
            double trX = (W - reachableAreaW * PX_PER_MM) / 2;
            double trY = (H - reachableAreaH * PX_PER_MM) / 2;
            zoom.setZoolLevelAndTranslation(zoomLevel, trX, trY);
            repaint();
            return;
        }
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);

        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, (int) (reachableAreaW * PX_PER_MM), (int) (reachableAreaH * PX_PER_MM));

        g.setColor(Color.MAGENTA);
        g.drawRect(0, 0, (int) (reachableAreaW * PX_PER_MM), (int) (reachableAreaH * PX_PER_MM));
        g.drawString("Reachable area", 0, g.getFontMetrics().getAscent());

        g.setColor(Color.WHITE);
        g.fillRect((int) (DRAWING_AREA_X * PX_PER_MM), (int) (DRAWING_AREA_Y * PX_PER_MM),
                (int) (DRAWING_AREA_WIDTH * PX_PER_MM), (int) (DRAWING_AREA_HEIGHT * PX_PER_MM));

        g.setColor(Color.MAGENTA);
        g.drawRect((int) (DRAWING_AREA_X * PX_PER_MM), (int) (DRAWING_AREA_Y * PX_PER_MM),
                (int) (DRAWING_AREA_WIDTH * PX_PER_MM), (int) (DRAWING_AREA_HEIGHT * PX_PER_MM));
        g.drawString("Drawing area", (int) (DRAWING_AREA_X * PX_PER_MM),
                (int) (DRAWING_AREA_Y * PX_PER_MM + g.getFontMetrics().getAscent()));

        paintCompletedJobs(g);

        paintStreamingJob(g);

        paintPreviewingJob(g);

        paintParkedTools(g);

        paintToolHead(g);
    }

    /**
     * Paint the already completed Jobs
     * 
     * @param g the Graphics2D with which to paint the Jobs
     */
    private void paintCompletedJobs(Graphics2D g) {
        Vector<Job> completedJobs = StreamingManager.Instance.getCompletedJobs();
        for (Job job : completedJobs) {
            HashMap<Ink, Instruction[]> instructionsPerInk = job.getInstructionsPerInk();
            for (Entry<Ink, Instruction[]> entry : instructionsPerInk.entrySet()) {
                Color inkColor = entry.getKey().getColor();
                Instruction previousInstruction = null;
                for (Instruction instruction : entry.getValue()) {
                    if (instruction.isDrawingMotion()) {
                        paintInstruction(g, job, previousInstruction, instruction, PEN_STROKE, inkColor);
                    }
                    previousInstruction = instruction;
                }
            }
        }
    }

    /**
     * Paint the currently streaming Jobs
     * 
     * @param g the Graphics2D with which to paint the Job
     */
    private void paintStreamingJob(Graphics2D g) {
        Job streamingJob = StreamingManager.Instance.getStreamingJob();
        if (streamingJob != null) {
            HashMap<Ink, Instruction[]> instructionsPerInk = streamingJob.getInstructionsPerInk();
            HashMap<Ink, Integer> nbExecutedInstructionsPerInk = streamingJob.getNbExecutedInstructionsPerInk();
            for (Entry<Ink, Instruction[]> entry : instructionsPerInk.entrySet()) {
                Instruction[] instructions = entry.getValue();
                int nbExecutedInstructions = nbExecutedInstructionsPerInk.get(entry.getKey());
                Color inkColor = entry.getKey().getColor();
                Instruction previousInstruction = null;
                for (int idInstr = 0; idInstr < nbExecutedInstructions; idInstr++) {
                    Instruction instruction = instructions[idInstr];
                    if (instruction.isDrawingMotion()) {
                        paintInstruction(g, streamingJob, previousInstruction, instruction, PEN_STROKE, inkColor);
                    }
                    previousInstruction = instruction;
                }
                for (int idInstr = nbExecutedInstructions + 1; idInstr < instructions.length; idInstr++) {
                    Instruction instruction = instructions[idInstr];
                    if (instruction.isDrawingMotion()) {
                        paintInstruction(g, streamingJob, previousInstruction, instruction, PEN_STROKE,
                                Utils.getLighterColor(inkColor));
                    } else if (SHOW_FLYING_MOTIONS) {
                        paintInstruction(g, streamingJob, previousInstruction, instruction, FLYING_STROKE,
                                FLYING_MOTIONS_COLOR);
                    }
                    previousInstruction = instruction;
                }
            }
        }
    }

    /**
     * Paint the previewing Job
     * 
     * @param g the Graphics2D with which to paint the Job
     */
    private void paintPreviewingJob(Graphics2D g) {
        if (previewingJob != null) {
            HashMap<Ink, Instruction[]> instructionsPerInk = previewingJob.getInstructionsPerInk();
            for (Entry<Ink, Instruction[]> entry : instructionsPerInk.entrySet()) {
                Color inkColor = entry.getKey().getColor();
                Instruction previousInstruction = null;
                for (Instruction instruction : entry.getValue()) {
                    if (instruction.isDrawingMotion()) {
                        paintInstruction(g, previewingJob, previousInstruction, instruction, PEN_STROKE, inkColor);
                    } else if (SHOW_FLYING_MOTIONS) {
                        paintInstruction(g, previewingJob, previousInstruction, instruction, FLYING_STROKE,
                                FLYING_MOTIONS_COLOR);
                    }
                    previousInstruction = instruction;
                }
            }
            g.setColor(Color.MAGENTA);
            double[] translatedOrigin = previewingJob.getTranslatedOrigin();

            double reachableAreaH = PlotterConfiguration.Instance
                    .getDoubleSettingValue(PlotterSetting.REACHABLE_AREA_HEIGHT);
            double jobWidth = previewingJob.getWidth();
            double jobHeight = previewingJob.getHeight();
            g.drawRect((int) (translatedOrigin[0] * PX_PER_MM),
                    (int) ((reachableAreaH - (translatedOrigin[1] + jobHeight)) * PX_PER_MM),
                    (int) (jobWidth * PX_PER_MM), (int) (jobHeight * PX_PER_MM));
            // TODO-030: do something more user-friendly instead of this simple square
            g.fillRect((int) (translatedOrigin[0] * PX_PER_MM) - 200,
                    (int) ((reachableAreaH - (translatedOrigin[1] + jobHeight)) * PX_PER_MM) - 200, 400, 400);
        }
    }

    /**
     * Paint an Instruction
     * 
     * @param g                   the Graphics2D with which to paint the Instruction
     * @param job                 the Job containing the Instruction to paint
     * @param previousInstruction the previous Instruction within the Job
     * @param instruction         the Instruction to paint
     * @param stroke              the stroke with which to pain the Instruction
     * @param color               the color with which to paint the Instruction
     */
    private void paintInstruction(Graphics2D g, Job job, Instruction previousInstruction, Instruction instruction,
            Stroke stroke, Color color) {
        double reachableAreaH = PlotterConfiguration.Instance
                .getDoubleSettingValue(PlotterSetting.REACHABLE_AREA_HEIGHT);
        double[] jobOffset = job.getTranslation();
        if (!instruction.isZAxisOnlyMotion()) {
            if (instruction.isLinearMotion()) {
                double[] startPosition = instruction.getStartPosition();
                double[] endPosition = instruction.getEndPosition();
                g.setStroke(stroke);
                g.setColor(color);
                g.drawLine((int) Math.round((startPosition[0] + jobOffset[0]) * PX_PER_MM),
                        (int) Math.round((reachableAreaH - (startPosition[1] + jobOffset[1])) * PX_PER_MM),
                        (int) Math.round((endPosition[0] + jobOffset[0]) * PX_PER_MM),
                        (int) Math.round((reachableAreaH - (endPosition[1] + jobOffset[1])) * PX_PER_MM));
            }
        }
    }

    /**
     * Paint the Plotter tool head
     * 
     * @param g the Graphics2D with which to paint
     */
    private void paintToolHead(Graphics2D g) {
        if (workPosition != null) {
            double toolRadius = 5;
            if (loadedTool.isAnActualTool()) {
                drawToolAt(g, loadedTool, workPosition[0], workPosition[1], workPosition[2]);
            } else {
                double reachableAreaH = PlotterConfiguration.Instance
                        .getDoubleSettingValue(PlotterSetting.REACHABLE_AREA_HEIGHT);
                g.setColor(Color.black);
                g.drawOval((int) Math.round((workPosition[0] - toolRadius) * PX_PER_MM),
                        (int) Math.round((reachableAreaH - workPosition[1] - toolRadius) * PX_PER_MM),
                        (int) (toolRadius * 2 * PX_PER_MM), (int) (toolRadius * 2 * PX_PER_MM));
            }
        }
    }

    /**
     * Paint the Plotter parked Tools
     * 
     * @param g the Graphics2D with which to paint
     */
    private void paintParkedTools(Graphics2D g) {
        Tool[] tools = PlotterConfiguration.Instance.getTools();
        for (Tool tool : tools) {
            if (!tool.equals(loadedTool)) {
                drawToolAt(g, tool, tool.getParkingX(), tool.getParkingY(), Double.MAX_VALUE);
            }
        }
    }

    /**
     * Draw the specified Tool at the specified position
     * 
     * @param g    the Graphics2D with which to paint the Tool
     * @param tool the Tool to paint
     * @param x    the Tool x coordinate
     * @param y    the Tool y coordinate
     * @param z    the Tool z coordinate
     */
    private void drawToolAt(Graphics2D g, Tool tool, double x, double y, double z) {
        double reachableAreaH = PlotterConfiguration.Instance
                .getDoubleSettingValue(PlotterSetting.REACHABLE_AREA_HEIGHT);
        Stroke defaultStroke = g.getStroke();
        g.setColor(tool.getInk().getColor());
        g.setStroke(Utils.getPenStrokeInMm(TOOL_WIDTH_IN_MM));
        g.drawOval((int) ((x - TOOL_WIDTH_IN_MM * 3 / 8) * PX_PER_MM),
                (int) ((reachableAreaH - y - TOOL_WIDTH_IN_MM * 3 / 8) * PX_PER_MM),
                (int) (TOOL_WIDTH_IN_MM * 3 / 4 * PX_PER_MM), (int) (TOOL_WIDTH_IN_MM * 3 / 4 * PX_PER_MM));

        g.setStroke(defaultStroke);

        double tipWidth = TOOL_WIDTH_IN_MM / 4 * Math.max(0, (SAFE_Z_HEIGHT - z) / SAFE_Z_HEIGHT);
        g.fillOval((int) ((x - tipWidth / 2) * PX_PER_MM), (int) ((reachableAreaH - y - tipWidth / 2) * PX_PER_MM),
                (int) (tipWidth * PX_PER_MM), (int) (tipWidth * PX_PER_MM));

    }

    /**
     * Set the currently previewing Job
     * 
     * @param previewingJob the new previewing Job
     */
    public void setPreviewingJob(Job previewingJob) {
        if (previewingJob == null || previewingJob.isCompatibleWithPlotter()) {
            if (previewingJob != null) {
                double reachableAreaH = PlotterConfiguration.Instance
                        .getDoubleSettingValue(PlotterSetting.REACHABLE_AREA_HEIGHT);
                double[] minPosition = previewingJob.getMinPosition();
                previewingJob.setTranslation(new double[] {
                        DRAWING_AREA_X - minPosition[0] + (DRAWING_AREA_WIDTH - previewingJob.getWidth()) / 2,
                        reachableAreaH - DRAWING_AREA_Y - minPosition[1]
                                - (DRAWING_AREA_HEIGHT + previewingJob.getHeight()) / 2 });
            }
            this.previewingJob = previewingJob;
            repaint();
        } else {
            System.out.println("Job incompatible with plotter");
        }
    }

    /**
     * Get the currently previewing Job
     * 
     * @return the currently previewing Job
     */
    public Job getPreviewingJob() {
        return previewingJob;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        /* nothing to do */
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            if (previewingJob != null) {
                double reachableAreaH = PlotterConfiguration.Instance
                        .getDoubleSettingValue(PlotterSetting.REACHABLE_AREA_HEIGHT);
                double[] translatedOrigin = previewingJob.getTranslatedOrigin();
                double jobTopLeftCornerX = translatedOrigin[0];
                double jobTopLeftCornerY = reachableAreaH - (translatedOrigin[1] + previewingJob.getHeight());
                double mouseXInMm = getZoomedMouseX(e) / PX_PER_MM;
                double mouseYInMm = getZoomedMouseY(e) / PX_PER_MM;

                // TODO-030: get rid of that 2mm magic number
                if (Math.abs(jobTopLeftCornerX - mouseXInMm) < 2 && Math.abs(jobTopLeftCornerY - mouseYInMm) < 2) {
                    translatingPreviewingJob = true;
                }
            }
        } else if (SwingUtilities.isRightMouseButton(e)) {
            // TODO-019: enable again when the popup functionnalities are implemented
            // popupMenu.show(viewPanel, e.getX(), e.getY());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        translatingPreviewingJob = false;

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        /* nothing to do */
    }

    @Override
    public void mouseExited(MouseEvent e) {
        /* nothing to do */
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        super.mouseDragged(e);
        if (translatingPreviewingJob) {
            double reachableAreaH = PlotterConfiguration.Instance
                    .getDoubleSettingValue(PlotterSetting.REACHABLE_AREA_HEIGHT);
            double[] tr = previewingJob.getTranslation();
            double[] minPosition = previewingJob.getMinPosition();
            double trX = tr[0] + (double) (e.getX() - previousMouseX) / zoom.getZoomLevel() / PX_PER_MM;
            // TODO-030: handle differently the -= to take into account the mirrored y axis?
            double trY = tr[1] - (double) (e.getY() - previousMouseY) / zoom.getZoomLevel() / PX_PER_MM;
            trX = Math.max(DRAWING_AREA_X - minPosition[0],
                    Math.min(DRAWING_AREA_X - minPosition[0] + DRAWING_AREA_WIDTH - previewingJob.getWidth(), trX));
            trY = Math.max(reachableAreaH - DRAWING_AREA_HEIGHT - DRAWING_AREA_Y - minPosition[1],
                    Math.min(reachableAreaH - DRAWING_AREA_Y - minPosition[1] - previewingJob.getHeight(), trY));

            previewingJob.setTranslation(new double[] { trX, trY });
            previousMouseX = e.getX();
            previousMouseY = e.getY();
            repaint();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object s = e.getSource();

        /* Move here menu item */
        if (s.equals(moveHereMenuItem)) {
            System.out.println("TODO-019: move here");
        }

        /* Clear drawing area */
        if (s.equals(clearDrawingAreaMenuItem)) {
            System.out.println("TODO-019: clear drawing area");
        }

        /* Set drawing area origin menu items */
        if (s.equals(bottomLeftDrawingAreaOriginMenuItem)) {
            System.out.println("TODO-019: drawing origin");
        }
        if (s.equals(topLeftDrawingAreaOriginMenuItem)) {
            System.out.println("TODO-019: drawing origin");
        }
        if (s.equals(bottomRightDrawingAreaOriginMenuItem)) {
            System.out.println("TODO-019: drawing origin");
        }
        if (s.equals(topRightDrawingAreaOriginMenuItem)) {
            System.out.println("TODO-019: drawing origin");
        }

        /* Set parking position menu item */
        if (s.equals(parkingPositionMenuItem)) {
            System.out.println("TODO-019: parking position");
        }

        /* Set tool 0 parking position menu item */
        if (s.equals(tool0ParkingPositionMenuItem)) {
            System.out.println("TODO-019: tool0 parking position");
        }
    }

    @Override
    public void plotterStatusChanged(PlotterStatus status) {
        /* nothing to do */
    }

    @Override
    public void loadedToolChanged(Tool newlyLoadedTool) {
        loadedTool = newlyLoadedTool;
    }

    @Override
    public void plotterDataChanged(double[] workPosition, double[] machinePosition, double currentFeedrate,
            boolean[] endstopsTriggered) {
        this.workPosition = workPosition;
        repaint();
    }

    @Override
    public void startedNewJob(Job job) {
        /* nothing to do */
    }

    @Override
    public void streamingProgressionChanged(double elapsedDurationSinceJobStart,
            HashMap<Ink, Integer> nbInstructionsExecutedPerInk, HashMap<Ink, Double> drawedDistancesPerInk,
            HashMap<Ink, Integer> estimatedRemainingDurationPerInk) {
        nbInstructionsExecutedPerInkForStreamingJob = nbInstructionsExecutedPerInk;
        repaint();
    }

    @Override
    public void jobCompleted() {
        /* nothing to do */
    }

}
