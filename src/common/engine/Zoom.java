package common.engine;

/**
 * A class representing a zoom, with its level and translation, that can be shared by multiple Views
 * 
 * @author Terence
 *
 */
public class Zoom {

    /**
     * The current zoom level
     */
    private double zoomLevel = 1;

    /**
     * The current translation of the View, linked to the zoom level
     */
    private double trX = 0, trY = 0;

    /**
     * A flag indicating if the zoom level or translation has been initialized (i.e. modified)
     */
    private boolean hasBeenInitialized = false;

    /**
     * Set the current zoom level
     * 
     * @param zoomLevel the new zoom level
     */
    public void setZoomLevel(double zoomLevel) {
        this.zoomLevel = zoomLevel;
        hasBeenInitialized = true;
    }

    /**
     * Set the current zoom translation
     * 
     * @param trX the new translation X coordinate
     * @param trY the new translation Y coordinate
     */
    public void setTranslation(double trX, double trY) {
        this.trX = trX;
        this.trY = trY;
        hasBeenInitialized = true;
    }

    /**
     * Set the zoom level and translation
     * 
     * @param zoomLevel the new zoom level
     * @param trX       the new translation X coordinate
     * @param trY       the new translation Y coordinate
     */
    public void setZoolLevelAndTranslation(double zoomLevel, double trX, double trY) {
        this.zoomLevel = zoomLevel;
        this.trX = trX;
        this.trY = trY;
        hasBeenInitialized = true;
    }

    /**
     * Get the current zoom level
     * 
     * @return the current zoom level
     */
    public double getZoomLevel() {
        return zoomLevel;
    }

    /**
     * Get the zoom X translation
     * 
     * @return the zoom X translation
     */
    public double getTranslationX() {
        return trX;
    }

    /**
     * Get the zoom Y translation
     * 
     * @return the zoom Y translation
     */
    public double getTranslationY() {
        return trY;
    }

    /**
     * Indicate if the zoom has been initialized
     * 
     * @return true if the zoom has been initialized, false otherwise
     */
    public boolean hasBeenInitialized() {
        return hasBeenInitialized;
    }
}
