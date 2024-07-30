package streaming.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

import common.engine.Ink;
import common.engine.Utils;
import common.gui.Window;
import streaming.engine.Job;
import streaming.engine.Tool;
import streaming.enums.PlotterStatus;
import streaming.listeners.PlotterDataListener;
import streaming.listeners.StreamingProgressionListener;
import streaming.session.PlotterConfiguration;
import streaming.session.StreamingManager;

/**
 * The Control Bar of the Streaming view, containing all the GUI components to connect to the machine and handle Jobs
 * 
 * @author Terence
 *
 */
public class ControlBar extends JPanel implements ActionListener, PlotterDataListener, StreamingProgressionListener {

    /**
     * The Serial port option to simulate a virtual Plotter that send back ACKs to every G-Code instruction
     */
    private final static String VIRTUAL_PLOTTER_SERIAL_PORT = "Virtual Plotter";

    /**
     * The Serial port combo selector
     */
    private final JComboBox<String> serialPortCombo;

    /**
     * The label showing the selected Job file name
     */
    private final JLabel filenameLabel;

    /**
     * The Control Bar buttons
     */
    private final JButton connectButton, disconnectButton, browseButton, runButton, cleanPauseButton, stopButton,
            emergencyPauseButton;

    /**
     * The JFileChoose used to open Job files
     */
    private final JFileChooser fileChooser;

    /**
     * Instantiate a new ControlBar
     */
    public ControlBar() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Plotter"));

        StreamingManager.Instance.addPlotterDataListener(this);
        StreamingManager.Instance.addStreamingProgressionListener(this);

        /* Serial settings */
        JPanel serialSettingsPanel = new JPanel();
        serialSettingsPanel.setLayout(new BoxLayout(serialSettingsPanel, BoxLayout.X_AXIS));
        serialSettingsPanel.setBorder(BorderFactory.createTitledBorder("Serial settings"));

        serialPortCombo = new JComboBox<String>(new String[] { VIRTUAL_PLOTTER_SERIAL_PORT });
        serialPortCombo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                serialPortCombo.removeAllItems();
                for (String port : StreamingManager.Instance.getAvailableStreamingPorts()) {
                    serialPortCombo.addItem(port);
                }
                serialPortCombo.addItem(VIRTUAL_PLOTTER_SERIAL_PORT);
                serialPortCombo.repaint();
            }
        });
        connectButton = new JButton("Connect");
        connectButton.setIcon(new ImageIcon("data/icons/connect.png"));
        connectButton.addActionListener(this);
        disconnectButton = new JButton("Disconnect");
        disconnectButton.setIcon(new ImageIcon("data/icons/disconnect.png"));
        disconnectButton.setEnabled(false);
        disconnectButton.addActionListener(this);

        serialSettingsPanel.add(new JLabel("Port:"));
        serialSettingsPanel.add(serialPortCombo);
        serialSettingsPanel.add(connectButton);
        serialSettingsPanel.add(disconnectButton);

        add(serialSettingsPanel, BorderLayout.WEST);

        /* File selection */
        JPanel fileSelectionPanel = new JPanel();
        fileSelectionPanel.setLayout(new BoxLayout(fileSelectionPanel, BoxLayout.X_AXIS));
        fileSelectionPanel.setBorder(BorderFactory.createTitledBorder("File selection"));

        filenameLabel = new JLabel("no file selected");
        browseButton = new JButton("Browse");
        browseButton.setIcon(new ImageIcon("data/icons/browse.png"));
        fileChooser = new JFileChooser(".");
        // TODO-023: add filter for gcode files
        browseButton.addActionListener(this);

        runButton = new JButton("Run");
        runButton.setIcon(new ImageIcon("data/icons/run.png"));
        runButton.setEnabled(false);
        runButton.addActionListener(this);

        cleanPauseButton = new JButton("Pause");
        cleanPauseButton.setIcon(new ImageIcon("data/icons/pause.png"));
        cleanPauseButton.setEnabled(false);
        cleanPauseButton.addActionListener(this);

        emergencyPauseButton = new JButton("Emergency pause");
        emergencyPauseButton.setIcon(new ImageIcon("data/icons/emergency-pause.png"));
        emergencyPauseButton.addActionListener(this);

        stopButton = new JButton("Stop");
        stopButton.setIcon(new ImageIcon("data/icons/stop.png"));
        stopButton.setEnabled(false);
        stopButton.addActionListener(this);

        fileSelectionPanel.add(filenameLabel);
        fileSelectionPanel.add(browseButton);
        fileSelectionPanel.add(runButton);
        // TODO-056: handle clean pause
        // fileSelectionPanel.add(cleanPauseButton);
        fileSelectionPanel.add(emergencyPauseButton);
        fileSelectionPanel.add(stopButton);

        add(fileSelectionPanel, BorderLayout.EAST);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object s = e.getSource();

        /* Connect button */
        if (s.equals(connectButton)) {
            String serialPort = (String) serialPortCombo.getSelectedItem();
            if (serialPort.equals(VIRTUAL_PLOTTER_SERIAL_PORT)) {
                StreamingManager.Instance.connectToVirtualPlotter();
            } else {
                StreamingManager.Instance.connectTo(serialPort);
            }
        }

        /* Disconnect button */
        if (s.equals(disconnectButton)) {
            StreamingManager.Instance.disconnect();
        }

        /* Browse button */
        if (s.equals(browseButton)) {
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                filenameLabel.setText(selectedFile.getPath());
                // TODO-020: do not use currently loaded tool, but set it as unspecified, and ask the user for the tool
                // before starting streaming it
                Tool tool = StreamingManager.Instance.getLoadedTool();
                if (!tool.isAnActualTool()) { // TODO-020: also remove that?
                    tool = PlotterConfiguration.Instance.getToolByInk(Ink.getBlackestAvailableInk());
                }
                String gcode;
                try {
                    gcode = Utils.readFile(selectedFile);
                    // TODO-023: check that the file is valid
                    Window.Instance.getPlotterView().setPreviewingJob(new Job(gcode, tool, false));
                    updateRunButtonEnabledStatus();
                } catch (IOException e1) {
                    System.err.println("Failed to open file");
                    e1.printStackTrace();
                }
            }
        }

        /* Run button */
        if (s.equals(runButton)) {
            if (StreamingManager.Instance.getStreamingJob() != null) {
                /* Resume paused streaming Job */
                System.out.println("resume job"); // TODO: remove log
                StreamingManager.Instance.resumeJob();
            } else {
                /* Start previewing Job */
                Job previewingJob = Window.Instance.getPlotterView().getPreviewingJob();
                if (previewingJob != null) {
                    Window.Instance.getPlotterView().setPreviewingJob(null);
                    StreamingManager.Instance.startJob(previewingJob);
                }
            }
        }

        /* Clean pause button */
        if (s.equals(cleanPauseButton)) {
            StreamingManager.Instance.pauseJob();
        }

        /* Emergency pause button */
        if (s.equals(emergencyPauseButton)) {
            StreamingManager.Instance.emergencyPause();
        }

        /* Stop button */
        if (s.equals(stopButton)) {
            StreamingManager.Instance.stopJob();
        }
    }

    /**
     * Enable the Run button if it should be clickable, disable it otherwise
     */
    private void updateRunButtonEnabledStatus() {
        boolean isPlotterConnected = StreamingManager.Instance.getPlotterStatus() != PlotterStatus.DISCONNECTED;
        boolean isCurrentToolKnown = StreamingManager.Instance.getLoadedTool() != Tool.UNDEFINED;
        Job streamingJob = StreamingManager.Instance.getStreamingJob();
        boolean isJobAlreadyStreaming = streamingJob != null && !streamingJob.isDone();
        Job previewingJob = Window.Instance.getPlotterView().getPreviewingJob();
        boolean isPreviewingJobSet = previewingJob != null;
        boolean isPaused = StreamingManager.Instance.getPlotterStatus() == PlotterStatus.HOLD;
        boolean canStartNewJob = !isJobAlreadyStreaming && isPreviewingJobSet;
        runButton.setEnabled(isPlotterConnected && isCurrentToolKnown && (canStartNewJob || isPaused));
    }

    @Override
    public void plotterStatusChanged(PlotterStatus status) {
        connectButton.setEnabled(status == PlotterStatus.DISCONNECTED);
        disconnectButton.setEnabled(status != PlotterStatus.DISCONNECTED);

        cleanPauseButton.setEnabled(status == PlotterStatus.RUN);
        emergencyPauseButton.setEnabled(status == PlotterStatus.RUN);
        stopButton.setEnabled(status == PlotterStatus.RUN || status == PlotterStatus.HOLD);

        updateRunButtonEnabledStatus();
    }

    @Override
    public void loadedToolChanged(Tool newlyLoadedTool) {
        updateRunButtonEnabledStatus();
    }

    @Override
    public void plotterDataChanged(double[] workPosition, double[] machinePosition, double currentFeedrate,
            boolean[] endstopsTriggered) {
        /* nothing to do */
    }

    @Override
    public void startedNewJob(Job job) {
        updateRunButtonEnabledStatus();
    }

    @Override
    public void streamingProgressionChanged(double elapsedDurationSinceJobStart,
            HashMap<Ink, Integer> nbInstructionsExecutedPerInk, HashMap<Ink, Double> drawedDistancesPerInk,
            HashMap<Ink, Integer> estimatedRemainingDurationsPerInk) {
        /* nothing to do */
    }

    @Override
    public void jobCompleted() {
        updateRunButtonEnabledStatus();
    }

}
