package image_processing.session;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import common.engine.SettingsSet;
import common.engine.Utils;
import image_processing.engine.BrushPalette;
import image_processing.enums.ColorAttributionMethod;
import image_processing.enums.ColorQuantizationMethod;
import image_processing.enums.ColorSelectionMethod;
import image_processing.enums.ColorSpace;
import image_processing.enums.OutputMirroring;
import image_processing.enums.OutputPosition;
import image_processing.enums.Setting;
import image_processing.listeners.ConfigurationChangeListener;

/**
 * A class representing an image-processing project configuration
 * 
 * @author Terence
 *
 */
public class Configuration {

    /**
     * The Configuration singleton
     */
    public static Configuration Instance;

    /**
     * The number of Brush Palettes available
     */
    public static int NB_BRUSH_PALETTES_AVAILABLE = 8;

    /**
     * The JSONObject containing the Configuration settings values
     */
    private JSONObject settings = new JSONObject();

    /**
     * The Configuration change listeners
     */
    private Vector<ConfigurationChangeListener> listeners = new Vector<ConfigurationChangeListener>();

    /**
     * Instantiate a new Configuration
     */
    private Configuration() {
        loadConfigurationFile(new File(".last-config.json"), false);
    }

    /**
     * Load a new Configuration by reading a JSON file, setting the missing Settings at their default value
     * 
     * @param configurationFile the JSON file from which to read the configuration
     * @param keepInputImage    set at true to not override the IMAGE_FILE Setting
     */
    public void loadConfigurationFile(File configurationFile, boolean keepInputImage) {
        String inputImage = settings.optString(Setting.IMAGE_FILE.getName(), "");
        settings = getDefaultSettings();
        String configurationFileContent = null;
        try {
            configurationFileContent = Utils.readFile(configurationFile);
        } catch (IOException e) {
            System.out.println("Failed to load last configuration, use default configuration");
        }
        if (configurationFileContent != null) {
            JSONObject userSettings = new JSONObject(configurationFileContent);
            for (String settingName : userSettings.keySet()) {
                settings.put(settingName, userSettings.get(settingName));
            }
        }
        if (keepInputImage && !inputImage.equals("")) {
            settings.put(Setting.IMAGE_FILE.getName(), inputImage);
        }
        fireConfigurationChanged(settings);
    }

    /**
     * Get a JSONObject containing all the Configuration Settings set at their default values
     * 
     * @return a JSONObject containing all the Configuration Settings set at their default values
     */
    private static JSONObject getDefaultSettings() {
        JSONObject settings = new JSONObject();
        settings.put(Setting.PEN_TIP_DIAMETER.getName(), 0.25);

        settings.put(Setting.IMAGE_FILE.getName(), "data/images/tulip.png");
        settings.put(Setting.IMAGE_DPI.getName(), 150);
        settings.put(Setting.COLOR_SPACE.getName(), ColorSpace.SRGB.ordinal());

        settings.put(Setting.NB_QUANTIZATION_COLORS.getName(), 8);
        settings.put(Setting.BLURRING_RADIUS.getName(), 0);
        settings.put(Setting.BLURRING_INTENSITY.getName(), 1.0);
        settings.put(Setting.COLOR_QUANTIZATION_METHOD.getName(), ColorQuantizationMethod.USE_AVAILABLE_INKS.ordinal());
        settings.put(Setting.COLOR_SELECTION_METHOD.getName(), ColorSelectionMethod.USE_MEDIAN_COLOR.ordinal());

        settings.put(Setting.LPMM_MAX.getName(), 4.0);
        settings.put(Setting.NB_LEVELS_PER_COLOR.getName(), 3);
        settings.put(Setting.COLOR_ATTRIBUTION_METHOD.getName(), ColorAttributionMethod.ASSIGN_CLOSEST_COLOR.ordinal());
        settings.put(Setting.GREY_SATURATION_THRESHOLD.getName(), 0.25);
        JSONArray brushPalettes = new JSONArray(NB_BRUSH_PALETTES_AVAILABLE);
        for (int idBrushPalette = 0; idBrushPalette < NB_BRUSH_PALETTES_AVAILABLE; idBrushPalette++) {
            brushPalettes.put(idBrushPalette, new BrushPalette().toJSonObject());
        }
        settings.put(Setting.BRUSH_PALETTES.getName(), brushPalettes);
        settings.put(Setting.ID_SELECTED_BRUSH_PALETTE.getName(), 0);

        settings.put(Setting.ENABLE_FINE_OUTLINING.getName(), false);
        settings.put(Setting.ENABLE_THICK_OUTLINING.getName(), false);
        settings.put(Setting.MULTICOLOR_THICK_OUTLINING.getName(), false);
        settings.put(Setting.OUTLINE_LPMM.getName(), 4.0);
        settings.put(Setting.CANNY_HIGH_THRESHOLD.getName(), 1);
        settings.put(Setting.CANNY_LOW_THRESHOLD.getName(), 0.5);

        settings.put(Setting.MIN_SEGMENT_LENGTH.getName(), 0);
        settings.put(Setting.CLEAR_Z_HEIGHT.getName(), 4.0);
        settings.put(Setting.OUTPUT_POSITION.getName(), OutputPosition.CENTERED_IN_A4.ordinal());
        settings.put(Setting.OUTPUT_ROTATION.getName(), 0);
        settings.put(Setting.OUTPUT_MIRRORING.getName(), OutputMirroring.X_MIRRORING.ordinal());
        return settings;
    }

    /**
     * Set a setting value
     * 
     * @param setting the setting to set
     * @param value   the new setting value
     */
    public void setSettingValue(Setting setting, Object value) {
        JSONObject settings = new JSONObject();
        settings.put(setting.getName(), value);
        setSettingsValues(settings);
    }

    /**
     * Set multiple settings values at once
     * 
     * TODO-053: keep using a JSONObject?-> no, pass a HashMap<Setting, Object>?
     * 
     * @param settings a JSONObject containing the settings to set and their new values
     */
    public void setSettingsValues(JSONObject settings) {
        for (String settingName : settings.keySet()) {
            this.settings.put(settingName, settings.get(settingName));
        }
        fireConfigurationChanged(settings);
    }

    /**
     * Set an array-type setting value
     * 
     * TODO-053: do something cleaner
     * 
     * @param setting the arra-type setting to set
     * @param index   the index to set within the array
     * @param value   the new setting value
     */
    public void setArraySettingValue(Setting setting, int index, Object value) {
        settings.getJSONArray(setting.getName()).put(index, value);
        JSONObject settingJsonObject = new JSONObject();
        settingJsonObject.put(setting.getName(), settings.get(setting.getName()));
        fireConfigurationChanged(settingJsonObject);
    }

    /**
     * Get a SettingsSet containing the current Configuration settings
     * 
     * TODO-053: try to get rid of this function or at least minimize its usage
     * 
     * @return a SettingsSet containing the current configuration settings and their values
     */
    public SettingsSet getCurrentSettings() {
        return new SettingsSet(settings);
    }

    /**
     * Add a Configuration change listener
     * 
     * @param listener the listener to add
     */
    public void addListener(ConfigurationChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Notify the listeners of a Configuration change
     * 
     * @param changedSettings a JSONObject containing the Settings that have changed
     */
    private void fireConfigurationChanged(JSONObject changedSettings) {
        for (ConfigurationChangeListener listener : listeners) {
            listener.configurationSettingsValuesChanged(new SettingsSet(changedSettings));
        }
        save(".last-config");
    }

    /**
     * Save the configuration as a file
     * 
     * @param filename the filename under which to save the configuration
     */
    public void save(String filename) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename + ".json"));
            bw.write(settings.toString());
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieve the available preset Configuration files
     * 
     * @return an HashMap containing the preset Configurations names and their corresponding Configuration files
     */
    public static HashMap<String, File> getAvailablePresetConfigurations() {
        HashMap<String, File> availablePresetConfigurations = new HashMap<String, File>();

        for (File file : new File("data/presets/").listFiles()) {
            if (file.getName().endsWith(".json")) {
                String presetName = file.getName();
                presetName = presetName.substring(0, presetName.length() - ".json".length()).replaceAll("-", " ");
                presetName = presetName.substring(0, 1).toUpperCase() + presetName.substring(1);
                availablePresetConfigurations.put(presetName, file);
            }
        }
        return availablePresetConfigurations;
    }

    /**
     * Init the Configuration Singleton
     */
    public static void initSingleton() {
        if (Instance == null) {
            Instance = new Configuration();
        }
    }

}
