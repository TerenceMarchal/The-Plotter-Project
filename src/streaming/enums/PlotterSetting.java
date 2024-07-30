package streaming.enums;

/**
 * The Plotter Configuration settings
 * 
 * @author Terence
 *
 */
public enum PlotterSetting {

    /* Read only from Plotter configuration file */
    IS_CORE_XY, DRAWING_XY_MAX_SPEED, JOG_SPEED, DRAWING_AREA_WIDTH, DRAWING_AREA_HEIGHT, DRAWING_AREA_X,
    DRAWING_AREA_Y, PARKING_X, PARKING_Y, PARKING_Z,

    /* Read from the configuration file but then overridden by the Plotter configuration */
    XY_ACCELERATION, Z_ACCELERATION, FLYING_XY_MAX_SPEED, Z_MAX_SPEED, REACHABLE_AREA_WIDTH, REACHABLE_AREA_HEIGHT;

    /**
     * Get the setting human-friendly name
     * 
     * @return the setting human-friendly name
     */
    public String getName() {
        return name().toLowerCase();
    }
}
