package streaming.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;

import common.engine.Ink;
import common.engine.Utils;
import streaming.engine.Job;
import streaming.engine.Tool;
import streaming.enums.PlotterSetting;
import streaming.enums.PlotterStatus;
import streaming.listeners.PlotterDataListener;
import streaming.listeners.StreamingProgressionListener;
import streaming.session.PlotterConfiguration;
import streaming.session.StreamingManager;

/**
 * The Manual Control Panel, used to send manual motions, macro and tool change instructions to the Plotter
 * 
 * @author Terence
 *
 */
public class ManualControlPanel extends JPanel
        implements ActionListener, StreamingProgressionListener, PlotterDataListener {

    /**
     * The manual motions buttons
     */
    private JButton upButton, downButton, rightButton, leftButton, upZButton, downZButton, upLeftButton, upRightButton,
            downLeftButton, downRightButton;

    /**
     * The manual motions distance spinners
     */
    JSpinner xyDistanceSpinner, zDistanceSpinner;

    /**
     * The macros buttons
     */
    private JButton macroHomeButton, macroUnlockButton, macroResetButton, macroParkButton, macroSetZeroButton;

    /**
     * The "no pen" toggle button for the pen selection
     */
    private JToggleButton noPenButton;

    /**
     * The pen selection button for each Ink
     */
    private HashMap<JToggleButton, Ink> penSelectionButtons = new HashMap<JToggleButton, Ink>();

    /**
     * The label used to warn about the current pen selection status
     */
    private JLabel penSelectionWarningLabel;

    /**
     * Instantiate a new Manual Control Panel
     */
    public ManualControlPanel() {
        StreamingManager.Instance.addStreamingProgressionListener(this);
        StreamingManager.Instance.addPlotterDataListener(this);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Manual control"));

        /* Basic motions control panel */
        JPanel basicMotions = new JPanel(new GridLayout(3, 4));
        basicMotions.setBorder(BorderFactory.createTitledBorder("Basic motions"));
        xyDistanceSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 500, 1.0));
        zDistanceSpinner = new JSpinner(new SpinnerNumberModel(1, 0.1, 500, 0.1));
        upButton = new JButton(new ImageIcon("data/icons/up.png"));
        upButton.addActionListener(this);
        downButton = new JButton(new ImageIcon("data/icons/down.png"));
        downButton.addActionListener(this);
        rightButton = new JButton(new ImageIcon("data/icons/right.png"));
        rightButton.addActionListener(this);
        leftButton = new JButton(new ImageIcon("data/icons/left.png"));
        leftButton.addActionListener(this);
        upZButton = new JButton(new ImageIcon("data/icons/up.png"));
        upZButton.addActionListener(this);
        downZButton = new JButton(new ImageIcon("data/icons/down.png"));
        downZButton.addActionListener(this);
        upLeftButton = new JButton("");
        upLeftButton.addActionListener(this);
        upRightButton = new JButton("");
        upRightButton.addActionListener(this);
        downLeftButton = new JButton("");
        downLeftButton.addActionListener(this);
        downRightButton = new JButton("");
        downRightButton.addActionListener(this);

        basicMotions.add(upLeftButton);
        basicMotions.add(upButton);
        basicMotions.add(upRightButton);
        basicMotions.add(upZButton);

        basicMotions.add(leftButton);
        basicMotions.add(xyDistanceSpinner);
        basicMotions.add(rightButton);
        basicMotions.add(zDistanceSpinner);

        basicMotions.add(downLeftButton);
        basicMotions.add(downButton);
        basicMotions.add(downRightButton);
        basicMotions.add(downZButton);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(basicMotions);

        /* Macros */
        JPanel macros = new JPanel(new GridLayout(2, 2));
        macros.setBorder(BorderFactory.createTitledBorder("Macros"));

        macroHomeButton = new JButton("Home");
        macroHomeButton.setIcon(new ImageIcon("data/icons/home.png"));
        macroHomeButton.addActionListener(this);
        macroHomeButton.setEnabled(false);
        macros.add(macroHomeButton);

        macroUnlockButton = new JButton("Unlock");
        macroUnlockButton.setIcon(new ImageIcon("data/icons/unlock.png"));
        macroUnlockButton.addActionListener(this);
        macroUnlockButton.setEnabled(false);
        macros.add(macroUnlockButton);

        macroResetButton = new JButton("Reset");
        macroResetButton.setIcon(new ImageIcon("data/icons/reset.png"));
        macroResetButton.addActionListener(this);
        macroResetButton.setEnabled(false);
        macros.add(macroResetButton);

        macroParkButton = new JButton("Park");
        macroParkButton.setIcon(new ImageIcon("data/icons/park.png"));
        macroParkButton.addActionListener(this);
        macros.add(macroParkButton);

        // TODO-013: check if it is possible to separate in set x/y zero and set z zero
        // TODO-013: do we need that? (yes for z axis, what about the x/y?)
        macroSetZeroButton = new JButton("Set Zero");
        macroSetZeroButton.setIcon(new ImageIcon("data/icons/zero.png"));
        macroSetZeroButton.addActionListener(this);
        macros.add(macroSetZeroButton);

        topPanel.add(macros);
        add(topPanel, BorderLayout.NORTH);

        /* Pen selection */
        JPanel penSelectionPanel = new JPanel(new BorderLayout());
        penSelectionPanel.setBorder(BorderFactory.createTitledBorder("Pen selection"));
        JPanel penSelectionButtonsPanel = new JPanel(new GridLayout(2, 4));

        ButtonGroup penButtonGroup = new ButtonGroup();

        noPenButton = new JToggleButton(new ImageIcon("data/icons/none.png"));
        penSelectionButtons.put(noPenButton, null);
        noPenButton.addActionListener(this);
        penButtonGroup.add(noPenButton);
        penSelectionButtonsPanel.add(noPenButton);

        for (Ink ink : Ink.getAvailableInks()) {
            JToggleButton penButton = new JToggleButton(Utils.getColorIcon(ink.getColor()));
            penSelectionButtons.put(penButton, ink);
            penButton.addActionListener(this);
            penButtonGroup.add(penButton);
            penSelectionButtonsPanel.add(penButton);

        }
        penSelectionPanel.add(penSelectionButtonsPanel, BorderLayout.CENTER);

        penSelectionWarningLabel = new JLabel("Please select the currently mounted tool");
        penSelectionWarningLabel.setForeground(Color.RED);
        penSelectionWarningLabel.setFont(getFont().deriveFont(Font.BOLD));
        penSelectionPanel.add(penSelectionWarningLabel, BorderLayout.SOUTH);

        add(penSelectionPanel, BorderLayout.SOUTH);

        plotterStatusChanged(PlotterStatus.DISCONNECTED);
    }

    /**
     * Enable or disable the manual controls
     * 
     * @param enableManualControls indicate if the manual controls should be enabled or disabled
     */
    private void setManualControlsEnabled(boolean enableManualControls) {
        upButton.setEnabled(enableManualControls);
        downButton.setEnabled(enableManualControls);
        rightButton.setEnabled(enableManualControls);
        leftButton.setEnabled(enableManualControls);
        upZButton.setEnabled(enableManualControls);
        downZButton.setEnabled(enableManualControls);
        upLeftButton.setEnabled(enableManualControls);
        upRightButton.setEnabled(enableManualControls);
        downLeftButton.setEnabled(enableManualControls);
        downRightButton.setEnabled(enableManualControls);

        macroHomeButton.setEnabled(enableManualControls);
        macroParkButton.setEnabled(enableManualControls);
        macroUnlockButton.setEnabled(enableManualControls);
        macroSetZeroButton.setEnabled(enableManualControls);
        macroResetButton.setEnabled(enableManualControls);

        noPenButton.setEnabled(enableManualControls);
        for (JToggleButton penButton : penSelectionButtons.keySet()) {
            penButton.setEnabled(enableManualControls);
        }

    }

    @Override
    public void startedNewJob(Job job) {
        setManualControlsEnabled(false);
    }

    @Override
    public void streamingProgressionChanged(double elapsedDurationSinceJobStart,
            HashMap<Ink, Integer> nbInstructionsExecutedPerInk, HashMap<Ink, Double> drawedDistancesPerInk,
            HashMap<Ink, Integer> estimatedRemainingDurationPerInk) {
        /* nothing to do */
    }

    @Override
    public void jobCompleted() {
        setManualControlsEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object s = e.getSource();
        /* manual motions buttons */
        double dx = 0;
        double dy = 0;
        double dz = 0;
        double xyDist = (double) xyDistanceSpinner.getValue();
        double zDist = (double) zDistanceSpinner.getValue();
        if (s.equals(upButton) || s.equals(upLeftButton) || s.equals(upRightButton)) {
            dy = xyDist;
        } else if (s.equals(downButton) || s.equals(downLeftButton) || s.equals(downRightButton)) {
            dy = -xyDist;
        }
        if (s.equals(rightButton) || s.equals(upRightButton) || s.equals(downRightButton)) {
            dx = xyDist;
        } else if (s.equals(leftButton) || s.equals(upLeftButton) || s.equals(downLeftButton)) {
            dx = -xyDist;
        }
        if (s.equals(upZButton)) {
            dz = zDist;
        } else if (s.equals(downZButton)) {
            dz = -zDist;
        }
        if (dx != 0 || dy != 0 || dz != 0) {
            StreamingManager.Instance.jog(dx, dy, dz,
                    Utils.speedToFeedrate(PlotterConfiguration.Instance.getDoubleSettingValue(PlotterSetting.JOG_SPEED),
                            PlotterConfiguration.Instance.getBooleanSettingValue(PlotterSetting.IS_CORE_XY)));
        }

        /* macros buttons */
        if (s.equals(macroHomeButton)) {
            StreamingManager.Instance.home();
        } else if (s.equals(macroUnlockButton)) {
            StreamingManager.Instance.unlock();
        } else if (s.equals(macroResetButton)) {
            StreamingManager.Instance.reset();
        } else if (s.equals(macroParkButton)) {
            StreamingManager.Instance.park();
        } else if (s.equals(macroSetZeroButton)) {
            System.out.println("TODO-013: set zero g-code");
        }

        /* pen selection */
        if (s.equals(noPenButton)) {
            if (StreamingManager.Instance.getLoadedTool() == Tool.UNDEFINED) {
                StreamingManager.Instance.setInitiallyLoadedTool(Tool.NONE);
            } else {
                StreamingManager.Instance.startChangeToolJob(Tool.NONE);
            }
        } else {
            for (Entry<JToggleButton, Ink> entry : penSelectionButtons.entrySet()) {
                if (s.equals(entry.getKey())) {
                    Tool tool = PlotterConfiguration.Instance.getToolByInk(entry.getValue());
                    if (StreamingManager.Instance.getLoadedTool() == Tool.UNDEFINED) {
                        StreamingManager.Instance.setInitiallyLoadedTool(tool);
                    } else {
                        StreamingManager.Instance.startChangeToolJob(tool);
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void plotterStatusChanged(PlotterStatus status) {
        /* Manual motions buttons */
        boolean manualMotionsEnabled = status == PlotterStatus.IDLE || status == PlotterStatus.JOG;
        upButton.setEnabled(manualMotionsEnabled);
        downButton.setEnabled(manualMotionsEnabled);
        rightButton.setEnabled(manualMotionsEnabled);
        leftButton.setEnabled(manualMotionsEnabled);
        upZButton.setEnabled(manualMotionsEnabled);
        downZButton.setEnabled(manualMotionsEnabled);
        upLeftButton.setEnabled(manualMotionsEnabled);
        upRightButton.setEnabled(manualMotionsEnabled);
        downLeftButton.setEnabled(manualMotionsEnabled);
        downRightButton.setEnabled(manualMotionsEnabled);

        /* Macros (except home, unlock and reset) buttons */
        boolean macrosEnabled = status == PlotterStatus.IDLE;
        macroParkButton.setEnabled(macrosEnabled);
        macroSetZeroButton.setEnabled(macrosEnabled);

        /* Home, unlock and reset buttons */
        // TODO-013: handle those 3 macros, and make sure that they can override current job
        boolean homeUnlockResetMacrosEnabled = status != PlotterStatus.DISCONNECTED;
        macroHomeButton.setEnabled(homeUnlockResetMacrosEnabled);
        macroUnlockButton.setEnabled(homeUnlockResetMacrosEnabled);
        macroResetButton.setEnabled(homeUnlockResetMacrosEnabled);
    }

    @Override
    public void loadedToolChanged(Tool newlyLoadedTool) {
        if (newlyLoadedTool != Tool.UNDEFINED) {
            penSelectionWarningLabel.setVisible(false);
            macroHomeButton.setEnabled(true);
            macroUnlockButton.setEnabled(true);
            macroResetButton.setEnabled(true);
        }
    }

    @Override
    public void plotterDataChanged(double[] workPosition, double[] machinePosition, double currentFeedrate,
            boolean[] endstopsTriggered) {
        /* nothing to do */
    }

}
