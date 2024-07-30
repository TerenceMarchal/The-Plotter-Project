package image_processing.listeners;

import common.engine.SettingsSet;

/**
 * A Listener to be notified when the configuration change
 * 
 * @author Terence
 *
 */
public interface ConfigurationChangeListener {

    /**
     * Callback called when some configuration settings change
     * 
     * @param settings a SettingsSet with the settings that have changed and their new values
     */
    public void configurationSettingsValuesChanged(SettingsSet settings);

}
