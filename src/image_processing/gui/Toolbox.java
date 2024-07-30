package image_processing.gui;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.json.JSONArray;
import org.json.JSONObject;

import common.engine.SettingsSet;
import image_processing.engine.Brush;
import image_processing.engine.BrushPalette;
import image_processing.enums.Setting;
import image_processing.generators.HelpGenerator;
import image_processing.listeners.BrushEditorListener;
import image_processing.listeners.ConfigurationChangeListener;
import image_processing.session.Configuration;
import image_processing.session.Project;

/**
 * The Toolbox, that contains all the settings, buttons, etc. used to configure the image transformations
 * 
 * @author Terence
 *
 */
public class Toolbox extends JPanel implements ConfigurationChangeListener, BrushEditorListener {

    /**
     * The TabbedPanel containing the Brush Editor panels
     */
    private JTabbedPane tabBrushEditorsPanels = new JTabbedPane();

    /**
     * Instantiate a new Toolbox
     */
    public Toolbox() {
        setBorder(BorderFactory.createTitledBorder("Toolbox"));
        JPanel allToolboxs = new JPanel();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(initGeneralSettingsToolbox());
        add(initBasicAndAdvancedSettingsToolbox());

        add(initBrushPaletteToolbox());
        add(initExportPanel());
    }

    /**
     * Init the General Settings Toolbox
     * 
     * @return the initialized General Settings Toolbox
     */
    private JPanel initGeneralSettingsToolbox() {
        JPanel toolbox = new JPanel(new GridLayout(0, 2));
        toolbox.setBorder(BorderFactory.createTitledBorder("General Settings"));

        JSpinner penTipDiameterSpinner = new JSpinner(new SpinnerNumberModel(0.5, 0.1, 10, 0.1));
        AdvancedSettingsPanel.linkComponentToSetting(penTipDiameterSpinner, Setting.PEN_TIP_DIAMETER,
                Configuration.Instance.getCurrentSettings());
        toolbox.add(HelpGenerator.getSettingLabelWithHelp(Setting.PEN_TIP_DIAMETER));
        toolbox.add(penTipDiameterSpinner);

        JButton importImageButton = new JButton("Import Image");
        importImageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser(new File("."));
                if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    JSONObject settings = new JSONObject();
                    settings.put(Setting.IMAGE_FILE.getName(), fileChooser.getSelectedFile().getPath());
                    /* force recomputing brush palette */
                    JSONArray emptyBrushPalettesArray = new JSONArray();
                    for (int idBrushPalette = 0; idBrushPalette < Configuration.NB_BRUSH_PALETTES_AVAILABLE; idBrushPalette++) {
                        emptyBrushPalettesArray.put(new BrushPalette().toJSonObject());
                    }
                    settings.put(Setting.BRUSH_PALETTES.getName(), emptyBrushPalettesArray);
                    Configuration.Instance.setSettingsValues(settings);
                }
            }
        });
        toolbox.add(new JLabel());
        toolbox.add(importImageButton);

        JButton importConfigButton = new JButton("Import Configuration");
        importConfigButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser(new File("."));
                if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    // TODO-052: handle configuration file import
                    // Project.Instance.setConfiguration(new Configuration(fileChooser.getSelectedFile()));
                }
            }
        });
        // TODO-052: handle importing configuration, handle setting value change through linkComponentToSetting
        // toolbox.add(new JLabel());
        // toolbox.add(importConfigButton);

        return toolbox;
    }

    /**
     * Init the JTabbedPane containing the Basic and Advanced Settings Toolbox
     * 
     * @return the initialized Toolbox
     */
    private JTabbedPane initBasicAndAdvancedSettingsToolbox() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Preset Configurations", initPresetConfigurationsToolbox());
        tabbedPane.addTab("Advanced Settings", new AdvancedSettingsPanel());
        return tabbedPane;
    }

    /**
     * Init the Preset Configurations Toolbox
     * 
     * @return the initialized Toolbox
     */
    private JPanel initPresetConfigurationsToolbox() {
        JPanel toolbox = new JPanel();
        toolbox.setLayout(new BoxLayout(toolbox, BoxLayout.Y_AXIS));

        ButtonGroup presetGroup = new ButtonGroup();
        for (Entry<String, File> entry : Configuration.getAvailablePresetConfigurations().entrySet()) {
            JRadioButton presetRadio = new JRadioButton(entry.getKey());
            presetRadio.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Configuration.Instance.loadConfigurationFile(entry.getValue(), true);
                }
            });
            presetGroup.add(presetRadio);
            toolbox.add(presetRadio);
        }

        return toolbox;
    }

    /**
     * Init the brush Palette Toolbox
     * 
     * @return the initialized Brush Palette Toolbox
     */
    private JPanel initBrushPaletteToolbox() {
        JPanel toolbox = new JPanel();
        toolbox.setBorder(BorderFactory.createTitledBorder("Brush Palettes"));
        toolbox.setLayout(new BoxLayout(toolbox, BoxLayout.Y_AXIS));

        tabBrushEditorsPanels.setTabPlacement(JTabbedPane.BOTTOM);
        Configuration.Instance.addListener(this);
        configurationSettingsValuesChanged(
                Configuration.Instance.getCurrentSettings()); /* load the initial brush palette */
        BrushPalette[] brushPalettes = Configuration.Instance.getCurrentSettings()
                .getBrushPalettesSetting(Setting.BRUSH_PALETTES);
        for (int idBrushPalette = 0; idBrushPalette < brushPalettes.length; idBrushPalette++) {
            BrushPaletteEditor brushPaletteEditor = new BrushPaletteEditor(this);
            brushPaletteEditor.setBrushPalette(brushPalettes[idBrushPalette]);
            tabBrushEditorsPanels.addTab("Palette " + (idBrushPalette + 1), brushPaletteEditor);
        }
        tabBrushEditorsPanels.setSelectedIndex(
                Configuration.Instance.getCurrentSettings().getIntSetting(Setting.ID_SELECTED_BRUSH_PALETTE));
        toolbox.add(tabBrushEditorsPanels);
        // TODO-037: shoud the tabs include the full configuration? or at least all the other settings related to brush
        // generation?
        tabBrushEditorsPanels.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Configuration.Instance.setSettingValue(Setting.ID_SELECTED_BRUSH_PALETTE,
                        tabBrushEditorsPanels.getSelectedIndex());
            }
        });

        return toolbox;
    }

    /**
     * Init the Export panel
     * 
     * @return the initialized Export panel
     */
    private JPanel initExportPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.setBorder(BorderFactory.createTitledBorder("Export"));

        JButton exportButton = new JButton("Export G-Code");
        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Project.Instance.getComputationThread().exportGcode();
            }
        });
        panel.add(exportButton);

        return panel;
    }

    @Override
    public void configurationSettingsValuesChanged(SettingsSet settings) {
        BrushPalette[] brushPalettes = settings.getBrushPalettesSetting(Setting.BRUSH_PALETTES);
        BrushPaletteEditor selectedBrushPaletteEditor = (BrushPaletteEditor) tabBrushEditorsPanels
                .getSelectedComponent();
        if (selectedBrushPaletteEditor != null && brushPalettes != null) {
            selectedBrushPaletteEditor.setBrushPalette(brushPalettes[tabBrushEditorsPanels.getSelectedIndex()]);
        }

    }

    @Override
    public void brushChanged(Brush brush) {
        BrushPaletteEditor selectedBrushPaletteEditor = (BrushPaletteEditor) tabBrushEditorsPanels
                .getSelectedComponent();
        Configuration configuration = Configuration.Instance;
        configuration.setArraySettingValue(Setting.BRUSH_PALETTES, tabBrushEditorsPanels.getSelectedIndex(),
                selectedBrushPaletteEditor.getBrushPalette().toJSonObject());
    }

}
