package streaming.session;

import java.io.File;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import common.engine.Ink;
import common.engine.Utils;
import streaming.engine.Tool;
import streaming.enums.PlotterSetting;

/**
 * A class representing a Plotter-related Configuration
 * 
 * @author Terence
 *
 */
public class PlotterConfiguration {

    /**
     * The Plotter Configuration singleton
     */
    public static PlotterConfiguration Instance;

    /**
     * The path to the file where the Plotter configuration is stored as a JSON file
     */
    private static final String PLOTTER_CONFIGURATION_FILE = "plotter-configuration.json";

    /**
     * The available Tools of the Plotter
     */
    private Tool[] tools;

    /**
     * The JSONObject containing the Plotter Configuration settings values
     */
    private JSONObject settings;

    /**
     * Instantiate a new Plotter Configuration
     */
    private PlotterConfiguration() {
        try {
            settings = new JSONObject(Utils.readFile(new File(PLOTTER_CONFIGURATION_FILE)));
        } catch (IOException e) {
            System.err.println("Failed to read Plotter configuration file");
            System.exit(-1);
        }

        JSONArray inksJsonArray = settings.getJSONArray("inks");
        for (int idInk = 0; idInk < inksJsonArray.length(); idInk++) {
            Ink ink = (Ink) Ink.fromJsonObject(inksJsonArray.getJSONObject(idInk));
            if (!Ink.registerNewAvailableInk(ink)) {
                System.err.println(String.format(
                        "Failed to register ink %s, there is already an ink with the same color registered",
                        ink.getName()));
            }
        }

        JSONArray toolsJsonArray = settings.getJSONArray("tools");
        tools = new Tool[toolsJsonArray.length()];
        for (int idTool = 0; idTool < tools.length; idTool++) {
            tools[idTool] = (Tool) Tool.fromJsonObject(toolsJsonArray.getJSONObject(idTool));
        }
    }

    /**
     * Get a setting value as a boolean
     * 
     * @param setting the setting to retrieve
     * @return the setting value as a boolean
     */
    public boolean getBooleanSettingValue(PlotterSetting setting) {
        return settings.has(setting.getName()) ? settings.getBoolean(setting.getName()) : false;
    }

    /**
     * Get a setting value as a double
     *
     * TODO-053: do something generic for both Configuration and PlotterConfiguration
     * 
     * @param setting the setting to retrieve
     * @return the setting value as a double
     */
    public double getDoubleSettingValue(PlotterSetting setting) {
        return settings.has(setting.getName()) ? settings.getDouble(setting.getName()) : 0.0;
    }

    /**
     * Get a setting value as a string
     * 
     * @param setting the setting to retrieve
     * @return the setting value as a string
     */
    public String getStringSettingValue(PlotterSetting setting) {
        return settings.has(setting.getName()) ? settings.getString(setting.getName()) : "";
    }

    /**
     * Override a double setting value, i.e. set its value but do no save it
     * 
     * @param setting the setting to overrode
     * @param value   the setting value as a double
     */
    public void overrideDoubleSettingValue(PlotterSetting setting, double value) {
        if (!settings.has(setting.getName()) || settings.getDouble(setting.getName()) != value) {
            System.out.println(String.format("Overriding %s, set at %f instead of %f", setting.getName(), value,
                    settings.getDouble(setting.getName())));
            settings.put(setting.getName(), value);
        }
    }

    /**
     * Get the Plotter available Tools
     * 
     * @return the Plotter available Tools
     */
    public Tool[] getTools() {
        return tools;
    }

    /**
     * Retrieve a Tool from its Ink
     * 
     * @param ink the Ink of the Tool
     * @return the Tool corresponding to the specified Ink, or Tool.UNDEFINED if the Ink is not available
     */
    public Tool getToolByInk(Ink ink) {
        for (Tool tool : tools) {
            if (tool.getInk().equals(ink)) {
                return tool;
            }
        }
        return Tool.UNDEFINED;
    }

    /**
     * Init the PlotterConfiguration Singleton
     */
    public static void initSingleton() {
        if (Instance == null) {
            Instance = new PlotterConfiguration();
        }
    }
}
