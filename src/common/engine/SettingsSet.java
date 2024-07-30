package common.engine;

import java.io.File;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import image_processing.engine.BrushPalette;
import image_processing.enums.ColorAttributionMethod;
import image_processing.enums.ColorQuantizationMethod;
import image_processing.enums.ColorSpace;
import image_processing.enums.OutputMirroring;
import image_processing.enums.OutputPosition;
import image_processing.enums.OutputRotation;
import image_processing.enums.Setting;

/**
 * An immutable set of Settings, representing a configuration at a certain time
 * 
 * @author Terence
 *
 */
public class SettingsSet implements Cloneable {

    /**
     * A JSONObject representing the settings tree and their values
     */
    private final JSONObject settings;

    /**
     * Instantiate a new immutable SettingsSet
     * 
     * @param settings the settings of the SettingSet
     */
    public SettingsSet(JSONObject settings) {
        this.settings = new JSONObject(settings.toString());
    }

    /**
     * Check if the SettingSet contain the specified Setting
     * 
     * @param setting the Setting for which to check the presence
     * @return true if the Setting is present in the SettingSet, false otherwise
     */
    public boolean has(Setting setting) {
        return settings.has(setting.getName());
    }

    /**
     * Get a Set containing the SettingSet Settings names
     * 
     * @return a Set containing the SettingSet Settings names
     */
    public Set<String> getSettingsNames() {
        return settings.keySet();
    }

    /**
     * Get a Setting value as a String
     * 
     * @param setting the Setting to retrieve
     * @return the Setting value as a String
     */
    public String getStringSetting(Setting setting) {
        String value = settings.getString(setting.getName());
        return value != null ? value : "";
    }

    /**
     * Get a Setting value as a File
     * 
     * @param setting the Setting to retrieve
     * @return the Setting value as a File
     */
    public File getFileSetting(Setting setting) {
        return settings.has(setting.getName()) ? new File(settings.getString(setting.getName())) : null;
    }

    /**
     * Get a Setting value as an int
     * 
     * @param setting the Setting to retrieve
     * @return the Setting value as an int
     */
    public int getIntSetting(Setting setting) {
        return settings.has(setting.getName()) ? settings.getInt(setting.getName()) : 0;
    }

    /**
     * Get a Setting value as a double
     * 
     * @param setting the Setting to retrieve
     * @return the Setting value as a double
     */
    public double getDoubleSetting(Setting setting) {
        return settings.has(setting.getName()) ? settings.getDouble(setting.getName()) : 0.0;
    }

    /**
     * Get a Setting value as a boolean
     * 
     * @param setting the Setting to retrieve
     * @return the Setting value as a boolean
     */
    public boolean getBoolSetting(Setting setting) {
        return settings.has(setting.getName()) ? settings.getBoolean(setting.getName()) : false;
    }

    /**
     * Get a Setting value as a ColorSpace
     * 
     * @param setting the Setting to retrieve
     * @return the Setting value as a ColorSpace
     */
    public ColorSpace getColorSpaceSetting(Setting setting) {
        return ColorSpace.values()[getIntSetting(setting)];
    }

    /**
     * Get a Setting value as a ColorQuantizationMethod
     * 
     * @param setting the Setting to retrieve
     * @return the Setting value as a ColorQuantizationMethod
     */
    public ColorQuantizationMethod getColorQuantizationMethodSetting(Setting setting) {
        return ColorQuantizationMethod.values()[getIntSetting(setting)];
    }

    /**
     * Get a Setting value as a ColorAttributionMethod
     * 
     * @param setting the Setting to retrieve
     * @return the Setting value as a ColorAttributionMethod
     */
    public ColorAttributionMethod getColorAttributionMethodSetting(Setting setting) {
        return ColorAttributionMethod.values()[getIntSetting(setting)];
    }

    /**
     * Get a Setting value as an OutputRotation
     * 
     * @param setting the Setting to retrieve
     * @return the Setting value as an OutputRotation
     */
    public OutputRotation getOutputRotationSetting(Setting setting) {
        return OutputRotation.values()[getIntSetting(setting)];
    }

    /**
     * Get a Setting value as an OutputPosition
     * 
     * @param setting the Setting to retrieve
     * @return the Setting value as an OutputPosition
     */
    public OutputPosition getOutputPositionSetting(Setting setting) {
        return OutputPosition.values()[getIntSetting(setting)];
    }

    /**
     * Get a Setting value as an OutputMirroring
     * 
     * @param setting the Setting to retrieve
     * @return the Setting value as an OutputMirroring
     */
    public OutputMirroring getOutputMirroringSetting(Setting setting) {
        return OutputMirroring.values()[getIntSetting(setting)];
    }

    /**
     * Get the currently selected BrushPalette
     * 
     * @return the currently selected BrushPalette
     */
    public BrushPalette getSelectedBrushPalette() {
        return getBrushPalettesSetting(Setting.BRUSH_PALETTES)[getIntSetting(Setting.ID_SELECTED_BRUSH_PALETTE)];
    }

    /**
     * Get a Setting value as a BrushPalette array from the specified JSONObject
     * 
     * @param jsonObject the JSONObject from which to retrieve the setting
     * @param setting    the Setting to retrieve
     * @return the Setting value as a BrushPalette array, or null of the specified JSONObject doesn't contain the
     *         setting
     */
    public BrushPalette[] getBrushPalettesSetting(Setting setting) {
        if (!settings.has(setting.getName())) {
            return null;
        }
        JSONArray brushPalettesJsonArray = settings.getJSONArray(setting.getName());
        BrushPalette[] brushPalettes = new BrushPalette[brushPalettesJsonArray.length()];
        for (int idBrushPalette = 0; idBrushPalette < brushPalettesJsonArray.length(); idBrushPalette++) {
            brushPalettes[idBrushPalette] = (BrushPalette) BrushPalette
                    .fromJsonObject(brushPalettesJsonArray.getJSONObject(idBrushPalette));
        }
        return brushPalettes;

    }

    @Override
    public String toString() {
        return String.format("[SettingsSet: %s]", settings.toString());
    }

    @Override
    public Object clone() {
        return new SettingsSet(settings);
    }
}
