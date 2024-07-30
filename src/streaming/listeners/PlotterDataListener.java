package streaming.listeners;

import streaming.engine.Tool;
import streaming.enums.PlotterStatus;

/**
 * A Listener to be notified about new Plotter-related data
 * 
 * @author Terence
 *
 */
public interface PlotterDataListener {

    /**
     * Callback called when the Plotter status change
     * 
     * @param status the new Plotter status
     */
    public void plotterStatusChanged(PlotterStatus status);

    /**
     * Callback called when a new Tool has been loaded on the Plotter
     * 
     * @param newlyLoadedTool the newly loaded Tool
     */
    public void loadedToolChanged(Tool newlyLoadedTool);

    /**
     * Callback called when the Plotter telemetry data change
     * 
     * @param workPosition      the Plotter current work position
     * @param machinePosition   the Plotter current machine position
     * @param currentFeedrate   the Plotter current feedrate
     * @param endstopsTriggered an array indicating if the X,Y and Z endstops are currently triggered
     */
    public void plotterDataChanged(double[] workPosition, double[] machinePosition, double currentFeedrate,
            boolean[] endstopsTriggered);

}
