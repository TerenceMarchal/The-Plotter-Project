package streaming.gui;

import java.awt.Color;
import java.awt.Font;
import java.util.HashMap;
import java.util.Locale;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import common.engine.Ink;
import common.engine.Utils;
import common.enums.LabelType;
import streaming.engine.Job;
import streaming.engine.Tool;
import streaming.enums.PlotterStatus;
import streaming.listeners.PlotterDataListener;
import streaming.listeners.StreamingProgressionListener;
import streaming.session.StreamingManager;

/**
 * The Streaming Status Bar, used to show information about the Plotter status and position as well as the current
 * streaming Job progression
 * 
 * @author Terence
 *
 */
public class StreamingStatusBar extends JPanel implements PlotterDataListener, StreamingProgressionListener {

    /**
     * The Plotter status and positions labels
     */
    private JLabel plotterStatusLabel, xPositionLabel, yPositionLabel, zPositionLabel, feedrateLabel, endstopsLabel;

    /**
     * The ColoredProgressBar used to display the currently streaming Job progression
     */
    private ColoredProgressBar progressBar;

    /**
     * The labels used to display informations about the streaming Job progression
     */
    private JLabel elapsedDurationLabel, remainingDurationLabel, instructionsProgressionLabel, drawingProgressionLabel;

    /**
     * HashMaps containing the number of instructions and estimated durations per Ink of the currently streaming Job
     */
    private HashMap<Ink, Integer> nbInstructionsPerInk, estimatedDurationsPerInk;

    /**
     * An HashMap containing the drawing distances per Ink
     */
    private HashMap<Ink, Double> drawingDistancesPerInk;

    private boolean showWorkPosition = true;// TODO-019: allow this to be configured from the popup menu

    public StreamingStatusBar() {
        StreamingManager.Instance.addPlotterDataListener(this);
        StreamingManager.Instance.addStreamingProgressionListener(this);

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        Font boldFont = new JLabel().getFont().deriveFont(Font.BOLD);

        add(new JLabel("Status:"));
        plotterStatusLabel = new JLabel("Disconnected");
        plotterStatusLabel.setFont(boldFont);
        plotterStatusLabel.setForeground(Color.RED);
        add(plotterStatusLabel);

        add(new JLabel(" X:"));
        xPositionLabel = new JLabel("?");
        xPositionLabel.setFont(boldFont);
        add(xPositionLabel);

        add(new JLabel(" Y:"));
        yPositionLabel = new JLabel("?");
        yPositionLabel.setFont(boldFont);
        add(yPositionLabel);

        add(new JLabel(" Z:"));
        zPositionLabel = new JLabel("?");
        zPositionLabel.setFont(boldFont);
        add(zPositionLabel);

        add(new JLabel(" Feedrate:"));
        feedrateLabel = new JLabel("?");
        feedrateLabel.setFont(boldFont);
        add(feedrateLabel);

        add(new JLabel(" Endstops:"));
        endstopsLabel = new JLabel("?");
        endstopsLabel.setFont(boldFont);
        add(endstopsLabel);

        progressBar = new ColoredProgressBar();
        add(progressBar);

        add(new JLabel(" Elapsed time:"));
        elapsedDurationLabel = new JLabel("0:00:00");
        elapsedDurationLabel.setFont(boldFont);
        add(elapsedDurationLabel);

        add(new JLabel(" Remaining time:"));
        remainingDurationLabel = new JLabel("0:00:00");
        remainingDurationLabel.setFont(boldFont);
        add(remainingDurationLabel);

        add(new JLabel(" Instructions:"));
        instructionsProgressionLabel = new JLabel("0/0");
        instructionsProgressionLabel.setFont(boldFont);
        add(instructionsProgressionLabel);

        add(new JLabel(" Drawing:"));
        drawingProgressionLabel = new JLabel("0m/0m");
        drawingProgressionLabel.setFont(boldFont);
        add(drawingProgressionLabel);

    }

    @Override
    public void plotterStatusChanged(PlotterStatus status) {
        String label = (status.name().charAt(0) + "").toUpperCase() + status.name().substring(1);
        Color foreground = Color.black;
        Color background = null;
        switch (status) {
        case DISCONNECTED:
            foreground = Color.RED;
            break;
        case IDLE:
            foreground = Color.GREEN;
            break;
        case RUN:
            background = Color.BLUE;
            break;
        case HOLD:
            background = Color.YELLOW;
            break;
        case JOG:
            background = Color.BLUE;
            break;
        case ALARM:
            background = Color.RED;
            break;
        case DOOR:
            background = Color.ORANGE;
            break;
        case CHECK:
            background = Color.YELLOW;
            break;
        case HOME:
            background = Color.YELLOW;
            break;
        case SLEEP:
            foreground = Color.YELLOW;
            break;
        case SIMULATING:
            background = Color.BLUE;
            break;
        }
        plotterStatusLabel.setText(label);
        if (background != null) {
            plotterStatusLabel.setForeground(Color.WHITE);
            plotterStatusLabel.setBackground(background);
            plotterStatusLabel.setOpaque(true);
        } else {
            plotterStatusLabel.setForeground(foreground);
            plotterStatusLabel.setOpaque(false);
        }
    }

    @Override
    public void loadedToolChanged(Tool newlyLoadedTool) {
        /* nothing to do */
    }

    @Override
    public void plotterDataChanged(double[] workPosition, double[] machinePosition, double currentFeedrate,
            boolean[] endstopsTriggered) {
        double x = showWorkPosition ? workPosition[0] : machinePosition[0];
        if (x != Double.NaN) {
            xPositionLabel.setText(String.format(Locale.US, "%6s", String.format(Locale.US, "%.2f", x)));
            xPositionLabel.setToolTipText(
                    String.format(Locale.US, "<html>Work position: <b>%.2f</b><br/>Machine position: <b>%.2f</b>",
                            workPosition[0], machinePosition[0]));
            double y = showWorkPosition ? workPosition[1] : machinePosition[1];
            yPositionLabel.setText(String.format(Locale.US, "%6s", String.format(Locale.US, "%.2f", y)));
            yPositionLabel.setToolTipText(
                    String.format(Locale.US, "<html>Work position: <b>%.2f</b><br/>Machine position: <b>%.2f</b>",
                            workPosition[0], machinePosition[0]));
            double z = showWorkPosition ? workPosition[2] : machinePosition[2];
            zPositionLabel.setText(String.format(Locale.US, "%5s", String.format(Locale.US, "%.2f", z)));
            zPositionLabel.setToolTipText(
                    String.format(Locale.US, "<html>Work position: <b>%.2f</b><br/>Machine position: <b>%.2f</b>",
                            workPosition[0], machinePosition[0]));
            feedrateLabel.setText(String.format(Locale.US, "%4s", String.format(Locale.US, "%.0f", currentFeedrate)));
        } else {
            xPositionLabel.setText("     ?");
            xPositionLabel.setToolTipText(null);
            yPositionLabel.setText("     ?");
            yPositionLabel.setToolTipText(null);
            zPositionLabel.setText("    ?");
            zPositionLabel.setToolTipText(null);
        }
        String endstops = "";
        for (int axis = 0; axis < 3; axis++) {
            if (endstopsTriggered[axis]) {
                endstops += (char) ('X' + axis) + " ";
            }
        }
        if (endstops.length() == 0) {
            endstops = "OK";
            endstopsLabel.setForeground(Color.BLACK);
        } else {
            endstopsLabel.setForeground(Color.RED);
        }
        endstopsLabel.setText(endstops);

    }

    @Override
    public void startedNewJob(Job job) {
        this.nbInstructionsPerInk = job.getNbInstructionsPerInk();
        this.drawingDistancesPerInk = job.getDrawingDistancesPerInk();
        this.estimatedDurationsPerInk = job.getEstimatedDurationPerInk();
        progressBar.setColorOrder(job.getOrderedInks());
    }

    @Override
    public void streamingProgressionChanged(double elapsedDurationSinceJobStart,
            HashMap<Ink, Integer> nbInstructionsExecutedPerInk, HashMap<Ink, Double> drawedDistancesPerInk,
            HashMap<Ink, Integer> estimatedRemainingDurationPerInk) {
        int nbInstructions = 0;
        double drawingDistance = 0;
        int estimatedDuration = 0;
        int nbInstructionsExecuted = 0;
        double drawedDistance = 0;
        int estimatedRemainingDuration = 0;
        for (Ink ink : Ink.getAvailableInks()) {
            nbInstructions += nbInstructionsPerInk.getOrDefault(ink, 0);
            drawingDistance += drawingDistancesPerInk.getOrDefault(ink, Double.valueOf(0));
            estimatedDuration += estimatedDurationsPerInk.getOrDefault(ink, 0);
            nbInstructionsExecuted += nbInstructionsExecutedPerInk.getOrDefault(ink, 0);
            drawedDistance += drawedDistancesPerInk.getOrDefault(ink, Double.valueOf(0));
            estimatedRemainingDuration += estimatedRemainingDurationPerInk.getOrDefault(ink, 0);
        }

        elapsedDurationLabel.setText(Utils.beautifyDurationAsClock(elapsedDurationSinceJobStart));
        remainingDurationLabel.setText(Utils.beautifyDurationAsClock(estimatedRemainingDuration));
        remainingDurationLabel.setToolTipText(
                Utils.generateIntValuePerInkTooltip(estimatedRemainingDurationPerInk, LabelType.DURATION));

        instructionsProgressionLabel
                .setText(Utils.beautifyProgression(nbInstructionsExecuted, nbInstructions, LabelType.DEFAULT));
        instructionsProgressionLabel.setToolTipText(Utils.generateIntProgressionPerInkTooltip(
                nbInstructionsExecutedPerInk, nbInstructionsPerInk, LabelType.DEFAULT));

        drawingProgressionLabel.setText(Utils.beautifyProgression(drawedDistance, drawingDistance, LabelType.DISTANCE));
        drawingProgressionLabel.setToolTipText(Utils.generateDoubleProgressionPerInkTooltip(drawedDistancesPerInk,
                drawingDistancesPerInk, LabelType.DISTANCE));

        // TODO-034: should we display duration/overall motions or drawing progression?
        progressBar.setProgress(drawedDistancesPerInk, drawingDistancesPerInk);

    }

    @Override
    public void jobCompleted() {
        /* nothing to do */
    }
}
