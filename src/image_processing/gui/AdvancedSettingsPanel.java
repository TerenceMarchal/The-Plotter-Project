package image_processing.gui;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.json.JSONArray;
import org.json.JSONObject;

import common.engine.SettingsSet;
import image_processing.engine.BrushPalette;
import image_processing.enums.OutputMirroring;
import image_processing.enums.OutputPosition;
import image_processing.enums.OutputRotation;
import image_processing.enums.Setting;
import image_processing.generators.HelpGenerator;
import image_processing.session.Configuration;

/**
 * A panel containing all the advanced settings to process the image into G-Code instructions
 * 
 * @author Terence
 *
 */
public class AdvancedSettingsPanel extends JScrollPane {

    public AdvancedSettingsPanel() {
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        getVerticalScrollBar().setUnitIncrement(16);
        JPanel allToolboxs = new JPanel();
        allToolboxs.setLayout(new BoxLayout(allToolboxs, BoxLayout.Y_AXIS));

        SettingsSet settingsValues = Configuration.Instance.getCurrentSettings();
        allToolboxs.add(initImageToolbox(settingsValues));
        allToolboxs.add(initColorQuantizationToolbox(settingsValues));
        allToolboxs.add(initBrushPaletteGenerationToolbox(settingsValues));
        allToolboxs.add(initOutliningToolbox(settingsValues));
        allToolboxs.add(initGCodeToolbox(settingsValues));

        add(allToolboxs);
        setViewportView(allToolboxs);
    }

    /**
     * Init the Image Toolbox
     * 
     * @param settingsValues the settings values to use to fill the GUI
     * @return the initialized Image Toolbox
     */
    private JPanel initImageToolbox(SettingsSet settingsValues) {
        JPanel toolbox = new JPanel(new GridLayout(0, 2));
        toolbox.setBorder(BorderFactory.createTitledBorder("Image"));

        JSpinner imageDpiSpinner = new JSpinner(new SpinnerNumberModel(150, 1, 600, 1));
        linkComponentToSetting(imageDpiSpinner, Setting.IMAGE_DPI, settingsValues);
        toolbox.add(HelpGenerator.getSettingLabelWithHelp(Setting.IMAGE_DPI));
        toolbox.add(imageDpiSpinner);

        toolbox.add(HelpGenerator.getSettingLabelWithHelp(Setting.COLOR_SPACE));
        ButtonGroup colorSpaceGroup = new ButtonGroup();
        JRadioButton rgbRadio = new JRadioButton("RGB");
        colorSpaceGroup.add(rgbRadio);
        toolbox.add(rgbRadio);
        toolbox.add(new JLabel());
        JRadioButton srgbRadio = new JRadioButton("sRGB");
        colorSpaceGroup.add(srgbRadio);
        toolbox.add(srgbRadio);
        toolbox.add(new JLabel());
        JRadioButton humanWeightedRadio = new JRadioButton("Human-Weighted");
        colorSpaceGroup.add(humanWeightedRadio);
        toolbox.add(humanWeightedRadio);
        linkComponentToSetting(colorSpaceGroup, Setting.COLOR_SPACE, settingsValues);

        return toolbox;
    }

    /**
     * Init the Color Quantization Toolbox
     * 
     * @param settingsValues the settings values to use to fill the GUI
     * @return the initialized Color Quantization Toolbox
     */
    private JPanel initColorQuantizationToolbox(SettingsSet settingsValues) {
        JPanel toolbox = new JPanel(new GridLayout(0, 2));
        toolbox.setBorder(BorderFactory.createTitledBorder("Color Quantization"));

        JSpinner nbColorsSpinner = new JSpinner();
        linkComponentToSetting(nbColorsSpinner, Setting.NB_QUANTIZATION_COLORS, settingsValues);
        toolbox.add(HelpGenerator.getSettingLabelWithHelp(Setting.NB_QUANTIZATION_COLORS));
        toolbox.add(nbColorsSpinner);

        JSpinner blurringRadiusSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 16, 1));
        linkComponentToSetting(blurringRadiusSpinner, Setting.BLURRING_RADIUS, settingsValues);
        toolbox.add(HelpGenerator.getSettingLabelWithHelp(Setting.BLURRING_RADIUS));
        toolbox.add(blurringRadiusSpinner);

        JSpinner blurringIntensitySpinner = new JSpinner(new SpinnerNumberModel(0.5, 0.5, 10, 0.5));
        linkComponentToSetting(blurringIntensitySpinner, Setting.BLURRING_INTENSITY, settingsValues);
        toolbox.add(HelpGenerator.getSettingLabelWithHelp(Setting.BLURRING_INTENSITY));
        toolbox.add(blurringIntensitySpinner);

        toolbox.add(HelpGenerator.getSettingLabelWithHelp(Setting.COLOR_QUANTIZATION_METHOD));
        ButtonGroup colorQuantizationMethodGroup = new ButtonGroup();
        JRadioButton useAvailableInksRadio = new JRadioButton("Use Available Inks");
        colorQuantizationMethodGroup.add(useAvailableInksRadio);
        toolbox.add(useAvailableInksRadio);
        toolbox.add(new JLabel(""));
        JRadioButton favorContrastsRadio = new JRadioButton("Favor Contrasts");
        colorQuantizationMethodGroup.add(favorContrastsRadio);
        toolbox.add(favorContrastsRadio);
        toolbox.add(new JLabel(""));
        JRadioButton favorColorTrueToOriginalsRadio = new JRadioButton("Favor Colors True to the Originals");
        colorQuantizationMethodGroup.add(favorColorTrueToOriginalsRadio);
        toolbox.add(favorColorTrueToOriginalsRadio);
        linkComponentToSetting(colorQuantizationMethodGroup, Setting.COLOR_QUANTIZATION_METHOD, settingsValues);

        toolbox.add(HelpGenerator.getSettingLabelWithHelp(Setting.COLOR_SELECTION_METHOD));
        ButtonGroup colorSelectionMethodGroup = new ButtonGroup();
        JRadioButton useMedianRadio = new JRadioButton("Median");
        colorSelectionMethodGroup.add(useMedianRadio);
        toolbox.add(useMedianRadio);
        toolbox.add(new JLabel(""));
        JRadioButton useAverageRadio = new JRadioButton("Average");
        colorSelectionMethodGroup.add(useAverageRadio);
        toolbox.add(useAverageRadio);
        linkComponentToSetting(colorSelectionMethodGroup, Setting.COLOR_SELECTION_METHOD, settingsValues);

        return toolbox;
    }

    /**
     * Init the brush Palette Generation Toolbox
     * 
     * @param settingsValues the settings values to use to fill the GUI
     * @return the initialized Brush Palette Generation Toolbox
     */
    private JPanel initBrushPaletteGenerationToolbox(SettingsSet settingsValues) {
        JPanel toolbox = new JPanel();
        toolbox.setBorder(BorderFactory.createTitledBorder("Brush Palette Generation"));
        toolbox.setLayout(new GridLayout(0, 2));

        JSpinner lpmmMaxSpinner = new JSpinner(new SpinnerNumberModel(0.5, 0.5, 16, 0.5));
        linkComponentToSetting(lpmmMaxSpinner, Setting.LPMM_MAX, settingsValues);
        toolbox.add(HelpGenerator.getSettingLabelWithHelp(Setting.LPMM_MAX));
        toolbox.add(lpmmMaxSpinner);

        JSpinner nbLevelsPerColorSpinner = new JSpinner();
        linkComponentToSetting(nbLevelsPerColorSpinner, Setting.NB_LEVELS_PER_COLOR, settingsValues);
        toolbox.add(HelpGenerator.getSettingLabelWithHelp(Setting.NB_LEVELS_PER_COLOR));
        toolbox.add(nbLevelsPerColorSpinner);
        toolbox.add(HelpGenerator.getSettingLabelWithHelp(Setting.COLOR_ATTRIBUTION_METHOD));
        ButtonGroup colorAttributionMethodGroup = new ButtonGroup();
        JRadioButton assignClosestColorRadio = new JRadioButton("Assign Closest Color");
        colorAttributionMethodGroup.add(assignClosestColorRadio);
        toolbox.add(assignClosestColorRadio);
        toolbox.add(new JLabel(""));
        JRadioButton favorMoreContrastsRadio = new JRadioButton("Favor More Contrasts");
        colorAttributionMethodGroup.add(favorMoreContrastsRadio);
        toolbox.add(favorMoreContrastsRadio);
        toolbox.add(new JLabel(""));
        JRadioButton assignClosestQuantizedInkRadio = new JRadioButton("Assign Closest Quantized Ink");
        colorAttributionMethodGroup.add(assignClosestQuantizedInkRadio);
        toolbox.add(assignClosestQuantizedInkRadio);
        linkComponentToSetting(colorAttributionMethodGroup, Setting.COLOR_ATTRIBUTION_METHOD, settingsValues);

        JSlider graySaturationThresholdSlider = new JSlider(0, 100);
        linkComponentToSetting(graySaturationThresholdSlider, Setting.GREY_SATURATION_THRESHOLD, settingsValues);
        toolbox.add(HelpGenerator.getSettingLabelWithHelp(Setting.GREY_SATURATION_THRESHOLD));
        toolbox.add(graySaturationThresholdSlider);

        JButton generateButton = new JButton("Generate Brush Palette");
        generateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JSONObject settings = new JSONObject();
                settings.put(Setting.NB_LEVELS_PER_COLOR.getName(), nbLevelsPerColorSpinner.getValue());
                settings.put(Setting.GREY_SATURATION_THRESHOLD.getName(),
                        graySaturationThresholdSlider.getValue() / 100.0);
                /* force the brush palette to be generated again */
                BrushPalette[] brushPalettes = settingsValues.getBrushPalettesSetting(Setting.BRUSH_PALETTES);
                int idSelectedBrushPalette = settingsValues.getIntSetting(Setting.ID_SELECTED_BRUSH_PALETTE);
                JSONArray brushPalettesJsonArray = new JSONArray(brushPalettes.length);
                for (int idBrushPalette = 0; idBrushPalette < brushPalettes.length; idBrushPalette++) {
                    brushPalettesJsonArray.put(idBrushPalette,
                            idBrushPalette != idSelectedBrushPalette ? brushPalettes[idBrushPalette].toJSonObject()
                                    : new BrushPalette().toJSonObject());
                }
                settings.put(Setting.BRUSH_PALETTES.getName(), brushPalettesJsonArray);
                Configuration.Instance.setSettingsValues(settings);

            }
        });

        toolbox.add(new JLabel(""));
        toolbox.add(generateButton);

        return toolbox;
    }

    /**
     * Init the Outlining Toolbox
     * 
     * @param settingsValues the settings values to use to fill the GUI
     * @return the initialized Outlining Toolbox
     */
    private JPanel initOutliningToolbox(SettingsSet settingsValues) {
        JPanel toolbox = new JPanel(new GridLayout(0, 2));
        toolbox.setBorder(BorderFactory.createTitledBorder("Outlining"));

        JComponentWithHelp enableFineOutliningCheckBox = HelpGenerator
                .getSettingCheckboxWithHelp(Setting.ENABLE_FINE_OUTLINING);
        linkComponentToSetting(enableFineOutliningCheckBox, Setting.ENABLE_FINE_OUTLINING, settingsValues);
        toolbox.add(enableFineOutliningCheckBox);
        toolbox.add(new JLabel());

        JComponentWithHelp enableThickOutliningCheckBox = HelpGenerator
                .getSettingCheckboxWithHelp(Setting.ENABLE_THICK_OUTLINING);
        linkComponentToSetting(enableThickOutliningCheckBox, Setting.ENABLE_THICK_OUTLINING, settingsValues);
        toolbox.add(enableThickOutliningCheckBox);

        JComponentWithHelp multicolorOutliningCheckBox = HelpGenerator
                .getSettingCheckboxWithHelp(Setting.MULTICOLOR_THICK_OUTLINING);
        linkComponentToSetting(multicolorOutliningCheckBox, Setting.MULTICOLOR_THICK_OUTLINING, settingsValues);
        toolbox.add(multicolorOutliningCheckBox);

        JSpinner highThresholdSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 3, 1));
        linkComponentToSetting(highThresholdSpinner, Setting.CANNY_HIGH_THRESHOLD, settingsValues);
        toolbox.add(HelpGenerator.getSettingLabelWithHelp(Setting.CANNY_HIGH_THRESHOLD));
        toolbox.add(highThresholdSpinner);

        JSpinner lowThresholdSpinner = new JSpinner(new SpinnerNumberModel(0.05, 0.05, 1, 0.05));
        linkComponentToSetting(lowThresholdSpinner, Setting.CANNY_LOW_THRESHOLD, settingsValues);
        toolbox.add(HelpGenerator.getSettingLabelWithHelp(Setting.CANNY_LOW_THRESHOLD));
        toolbox.add(lowThresholdSpinner);

        JSpinner outlineLppmSpinner = new JSpinner(new SpinnerNumberModel(0.5, 0.5, 12, 0.5));
        linkComponentToSetting(outlineLppmSpinner, Setting.OUTLINE_LPMM, settingsValues);
        toolbox.add(HelpGenerator.getSettingLabelWithHelp(Setting.OUTLINE_LPMM));
        toolbox.add(outlineLppmSpinner);

        return toolbox;
    }

    /**
     * Init the G-Code Toolbox
     * 
     * @param settingsValues the settings values to use to fill the GUI
     * @return the initialized G-Code Toolbox
     */
    private JPanel initGCodeToolbox(SettingsSet settingsValues) {
        JPanel toolbox = new JPanel(new GridLayout(0, 2));
        toolbox.setBorder(BorderFactory.createTitledBorder("G-Code"));

        JSpinner minSegmentLengthSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10, 0.1));
        linkComponentToSetting(minSegmentLengthSpinner, Setting.MIN_SEGMENT_LENGTH, settingsValues);
        toolbox.add(HelpGenerator.getSettingLabelWithHelp(Setting.MIN_SEGMENT_LENGTH));
        toolbox.add(minSegmentLengthSpinner);

        JSpinner clearZSpinner = new JSpinner(new SpinnerNumberModel(0.5, 0.5, 10, 0.5));
        linkComponentToSetting(clearZSpinner, Setting.CLEAR_Z_HEIGHT, settingsValues);
        toolbox.add(HelpGenerator.getSettingLabelWithHelp(Setting.CLEAR_Z_HEIGHT));
        toolbox.add(clearZSpinner);

        JComboBox<OutputPosition> outputPositionCombo = new JComboBox<OutputPosition>(OutputPosition.values());
        linkComponentToSetting(outputPositionCombo, Setting.OUTPUT_POSITION, settingsValues);
        toolbox.add(HelpGenerator.getSettingLabelWithHelp(Setting.OUTPUT_POSITION));
        toolbox.add(outputPositionCombo);

        JComboBox<OutputRotation> outputRotationCombo = new JComboBox<OutputRotation>(OutputRotation.values());
        linkComponentToSetting(outputRotationCombo, Setting.OUTPUT_ROTATION, settingsValues);
        toolbox.add(HelpGenerator.getSettingLabelWithHelp(Setting.OUTPUT_ROTATION));
        toolbox.add(outputRotationCombo);

        JComboBox<OutputMirroring> outputMirroringCombo = new JComboBox<OutputMirroring>(OutputMirroring.values());
        linkComponentToSetting(outputMirroringCombo, Setting.OUTPUT_MIRRORING, settingsValues);
        toolbox.add(HelpGenerator.getSettingLabelWithHelp(Setting.OUTPUT_MIRRORING));
        toolbox.add(outputMirroringCombo);

        return toolbox;
    }

    /**
     * Link a JComponent to a Setting, so that when the user change the component value, the setting is updated
     * accordingly, and the transformations recomputed if needed
     * 
     * @param component      the component to link to the setting
     * @param setting        the setting to link
     * @param settingsValues the settings values, from which to set the component initial value
     */
    protected static void linkComponentToSetting(JComponent component, Setting setting, SettingsSet settingsValues) {
        if (component instanceof JSpinner) {
            JSpinner comp = ((JSpinner) component);
            comp.setValue(settingsValues.getDoubleSetting(setting));
            comp.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    Configuration.Instance.setSettingValue(setting, comp.getValue());
                }
            });
        } else if (component instanceof JSlider) {
            JSlider comp = ((JSlider) component);
            comp.setValue((int) Math.round(settingsValues.getDoubleSetting(setting) * 100));
            comp.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    Configuration.Instance.setSettingValue(setting, (double) comp.getValue() / 100);
                }
            });
        } else if (component instanceof JCheckBox) {
            JCheckBox comp = ((JCheckBox) component);
            comp.setSelected(settingsValues.getBoolSetting(setting));
            comp.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Configuration.Instance.setSettingValue(setting, comp.isSelected());

                }
            });
        } else if (component instanceof JComboBox) {
            JComboBox comp = ((JComboBox) component);
            comp.setSelectedIndex(settingsValues.getIntSetting(setting));
            comp.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Configuration.Instance.setSettingValue(setting, comp.getSelectedIndex());

                }
            });
        } else if (component instanceof JComponentWithHelp) {
            JCheckBox comp = (JCheckBox) ((JComponentWithHelp) component).getComponent();
            comp.setSelected(settingsValues.getBoolSetting(setting));
            comp.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Configuration.Instance.setSettingValue(setting, comp.isSelected());

                }
            });
        } else {
            System.err.println(
                    "Unsupported component type for linkComponentToSetting: " + component.getClass().getSimpleName());
        }
    }

    /**
     * Link a ButtonGroup to a Setting, so that when the user select one of the ButtonGroup element, the setting is
     * updated accordingly, and the transformations recomputed if needed
     * 
     * @param buttonGroup    the ButtonGroup to link to the setting
     * @param setting        the setting to link
     * @param settingsValues the settings values, from which to set the component initial value
     */
    private static void linkComponentToSetting(ButtonGroup buttonGroup, Setting setting, SettingsSet settingsValues) {
        Enumeration<AbstractButton> elements = buttonGroup.getElements();
        int id = 0;
        while (elements.hasMoreElements()) {
            AbstractButton element = elements.nextElement();
            element.setSelected(id == settingsValues.getIntSetting(setting));
            int idElement = id++;
            element.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Configuration.Instance.setSettingValue(setting, idElement);
                }
            });
        }
    }
}
