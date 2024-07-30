package image_processing.listeners;

import image_processing.engine.Brush;

/**
 * A Listener to be notified when the user update a Brush through a Brush Editor
 * 
 * @author Terence
 *
 */
public interface BrushEditorListener {

    /**
     * Callback called when a Brush configuration changed
     * 
     * @param brush the updated Brush
     */
    public void brushChanged(Brush brush);

}
