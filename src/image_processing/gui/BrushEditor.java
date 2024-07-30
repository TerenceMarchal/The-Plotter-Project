package image_processing.gui;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSpinnerUI;

import common.engine.Ink;
import common.engine.Utils;
import image_processing.engine.Brush;
import image_processing.enums.Setting;
import image_processing.listeners.BrushEditorListener;
import image_processing.session.Configuration;

/**
 * A Brush Editor, to configure the settings of a Brush
 * 
 * @author Terence
 *
 */
public class BrushEditor extends JPanel {

    /**
     * The margins of the Brush Editor buttons
     */
    private static final Insets BUTTON_MARGINS = new Insets(0, 0, 0, 0);

    /**
     * The BrushEditorListener to notify when a Brush setting is updated by the user
     */
    private BrushEditorListener listener;

    /**
     * The Brush-related parameters
     * 
     * TODO-036: maybe we could use a non-final version of the Brush class to keep all these settings?
     */
    private int inputColor;
    private Ink ink;
    private double level;
    private int angle;
    private boolean fineOutliningEnabled;
    private boolean thickOutliningEnabled;

    /**
     * Instantiate a new Brush Editor
     * 
     * @param brush    the Brush from which getting the initial settings value
     * @param listener the BrushEditorListener to notify when one of the Brush setting is modified
     */
    public BrushEditor(Brush brush, BrushEditorListener listener) {
        this.inputColor = brush.getInputColor();
        this.ink = brush.getInk();
        this.level = brush.getLevel();
        this.angle = brush.getAngle();
        this.fineOutliningEnabled = brush.isFineOutliningEnabled();
        this.thickOutliningEnabled = brush.isThickOutliningEnabled();
        this.listener = listener;
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        /* Input color */
        JLabel inputColorLabel = new JLabel("                  ");
        inputColorLabel.setOpaque(true);
        inputColorLabel.setBackground(new Color(inputColor));
        add(inputColorLabel);

        /* Ink */
        JPanel inkColorPanel = new JPanel(new GridLayout(1, 0));
        ButtonGroup inkColorButtonGroup = new ButtonGroup();
        for (Ink ink : Ink.getAvailableInks()) {
            JToggleButton button = new JToggleButton(Utils.getColorIcon(ink.getColor()));
            button.setMargin(BUTTON_MARGINS);
            inkColorButtonGroup.add(button);
            button.setSelected(this.ink.equals(ink));
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!BrushEditor.this.ink.equals(ink)) {
                        BrushEditor.this.ink = ink;
                    } else {
                        BrushEditor.this.ink = null;
                    }
                    fireListener();
                }
            });
            inkColorPanel.add(button);
        }
        add(inkColorPanel);

        /* Filling Level */
        JSpinner fillingSpinner = new JSpinner(new SpinnerNumberModel(level, 0, 1, .05));
        fillingSpinner.setEditor(new JSpinner.NumberEditor(fillingSpinner, "0%"));
        fillingSpinner.setUI(new BasicSpinnerUI() {
            @Override
            protected JComponent createNextButton() {
                JButton buttonUp = (JButton) super.createNextButton();
                buttonUp.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        double valueDelta = 1.0 / Configuration.Instance.getCurrentSettings()
                                .getIntSetting(Setting.NB_LEVELS_PER_COLOR);
                        double value = ((double) fillingSpinner.getValue() / valueDelta + 1) * valueDelta;
                        fillingSpinner.setValue(Math.min(value, 1));
                    }
                });
                return buttonUp;
            }

            @Override
            protected JComponent createPreviousButton() {
                JButton buttonDown = (JButton) super.createPreviousButton();
                buttonDown.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        double valueDelta = 1.0 / Configuration.Instance.getCurrentSettings()
                                .getIntSetting(Setting.NB_LEVELS_PER_COLOR);
                        double value = ((double) fillingSpinner.getValue() / valueDelta - 1) * valueDelta;
                        fillingSpinner.setValue(Math.max(value, 0));
                    }
                });
                return buttonDown;
            }
        });
        fillingSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                BrushEditor.this.level = (double) fillingSpinner.getValue();
                fireListener();
            }
        });
        add(fillingSpinner);

        /* Filling pattern */
        JPanel patternPanel = new JPanel(new GridLayout(1, 0));
        ButtonGroup patternButtonGroup = new ButtonGroup();
        int[] angles = new int[] { 0, 90, 45, -45 };
        for (int angle : angles) {
            JToggleButton button = new JToggleButton(new ImageIcon("data/icons/filling-" + angle + "deg.png"));
            button.setMargin(BUTTON_MARGINS);
            patternButtonGroup.add(button);
            button.setSelected(this.angle == angle);
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    BrushEditor.this.angle = angle;
                    fireListener();
                }
            });
            patternPanel.add(button);
        }

        add(patternPanel);

        /* Outlining */
        JPanel outliningPanel = new JPanel(new GridLayout(1, 0));
        JToggleButton fineOutliningCheckbox = new JToggleButton(new ImageIcon("data/icons/fine-outlining.png"));
        fineOutliningCheckbox.setMargin(BUTTON_MARGINS);
        fineOutliningCheckbox.setSelected(fineOutliningEnabled);
        fineOutliningCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BrushEditor.this.fineOutliningEnabled = fineOutliningCheckbox.isSelected();
                fireListener();
            }
        });
        outliningPanel.add(fineOutliningCheckbox);

        JToggleButton thickOutliningCheckbox = new JToggleButton(new ImageIcon("data/icons/thick-outlining.png"));
        thickOutliningCheckbox.setMargin(BUTTON_MARGINS);
        thickOutliningCheckbox.setSelected(thickOutliningEnabled);
        thickOutliningCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BrushEditor.this.thickOutliningEnabled = thickOutliningCheckbox.isSelected();
                fireListener();
            }
        });
        outliningPanel.add(thickOutliningCheckbox);

        add(outliningPanel);
    }

    /**
     * Notify the BrushEditorListener that the Brush was modified by the user
     */
    private void fireListener() {
        listener.brushChanged(getBrush());
    }

    /**
     * Get the Brush corresponding to the configured settings
     * 
     * @return the Brush corresponding to the configured settings
     */
    public Brush getBrush() {
        return new Brush(inputColor, ink, level, angle, fineOutliningEnabled, thickOutliningEnabled);
    }

}
