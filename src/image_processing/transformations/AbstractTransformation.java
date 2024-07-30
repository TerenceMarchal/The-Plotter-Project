package image_processing.transformations;

import java.awt.Graphics2D;
import java.util.Set;
import java.util.Vector;

import common.engine.SettingsSet;
import image_processing.engine.ColorPalette;
import image_processing.engine.Image;
import image_processing.enums.Setting;
import image_processing.listeners.TransformationResultChangeListener;
import image_processing.session.Configuration;

/**
 * An abstract class for a Transformation, that apply a transformation algorithm depending on various parameters on the
 * previous transformation outputed image to output its own result image
 * 
 * @author Terence
 *
 */
public abstract class AbstractTransformation {

    /**
     * The TransformationStep corresponding to this Transformation
     */
    private final TransformationStep transformationStep;

    /**
     * The settings the Transformation depends on
     */
    private final Setting[] settingsDependingOn;

    /**
     * The latest settings values with which the Transformation has been computed
     */
    protected SettingsSet settingsValues;

    /**
     * The output image generated by the Transformation
     */
    private Image outputImage;

    /**
     * The output color palette generated by the Transformation
     */
    protected ColorPalette outputColorPalette;

    /**
     * The listeners waiting for a new output from the Transformation
     */
    protected Vector<TransformationResultChangeListener> listeners = new Vector<TransformationResultChangeListener>();

    /**
     * A flag indicating if the Transformation update should be aborted
     */
    protected boolean shouldAbortUpdate;

    /**
     * A flag indicating if the Transformation is complete
     */
    private boolean isTransformationDone;

    /**
     * A flag indicating if the output image is vectorized or not
     */
    private boolean isOutputImageVectorized;

    /**
     * Instantiate a Transformation
     * 
     * @param transformationStep      the corresponding Transformation step
     * @param settingsDependingOn     the settings on which the Transformation depend
     * @param isOutputImageVectorized indicate if the output image is vectorized
     */
    public AbstractTransformation(TransformationStep transformationStep, Setting[] settingsDependingOn,
            boolean isOutputImageVectorized) {
        this.transformationStep = transformationStep;
        this.settingsDependingOn = settingsDependingOn;
        this.isOutputImageVectorized = isOutputImageVectorized;
        this.settingsValues = Configuration.Instance.getCurrentSettings();
    }

    /**
     * Get the settings which the Transformation depends on
     * 
     * @return the settings which the Transformation depends on
     */
    public final Setting[] getSettingsDependingOn() {
        return settingsDependingOn;
    }

    /**
     * Indicate if the Transformation depend on at least one of the setting provided
     * 
     * @param settingsNames the names of the settings to check
     * @return true if the Transformation depend on at least one of the setting provided, false otherwise
     */
    public final boolean dependsOnSettings(Set<String> settingsNames) {
        for (Setting neededSetting : settingsDependingOn) {
            for (String settingToCheck : settingsNames)
                if (settingToCheck.equals(neededSetting.getName())) {
                    return true;
                }
        }
        return false;
    }

    /**
     * Add a listener on the Transformation result change
     * 
     * @param listener the listener to add
     */
    public final void addListener(TransformationResultChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Get the Transformation step corresponding to the Transformation
     * 
     * @return the Transformation step corresponding to the Transformation
     */
    public final TransformationStep getTransformationStep() {
        return transformationStep;
    }

    /**
     * Get the SettingsSet with which the Transformation has been computed
     * 
     * @return the SettingsSet with which the Transformation has been computed
     */
    public final SettingsSet getSettingsValues() {
        return settingsValues;
    }

    /**
     * Get the output image generated by the Transformation
     * 
     * @return the output image generated by the Transformation
     */
    public final Image getOutputImage() {
        return outputImage;
    }

    /**
     * Get the output color palette generated by the Transformation
     * 
     * @return the output color palette generated by the Transformation
     */
    public final ColorPalette getOutputColorPalette() {
        return outputColorPalette;
    }

    /**
     * Set the Transformation progression
     * 
     * @param label         a label indicating the Transformation current progression
     * @param progression   the current progression, between 0.0 and 1.0
     * @param shouldRepaint indicate if the Transformation current result can be repainted
     */
    protected final void setProgression(String label, double progression, boolean shouldRepaint) {
        for (TransformationResultChangeListener listener : listeners) {
            listener.transformationProgressionChanged(this, label, progression, shouldRepaint);
        }
    }

    /**
     * Set the Transformation progression
     * 
     * @param progression   the current progression, between 0.0 and 1.0
     * @param shouldRepaint indicate if the Transformation current result can be repainted
     */
    protected final void setProgression(double progression, boolean shouldRepaint) {
        setProgression(null, progression, shouldRepaint);
    }

    /**
     * Trigger the update of the Transformation
     * 
     * @param previousTransformation the previous Transformation, which this Transformation is then applied on the
     *                               output image
     * @param settings               the settings with which to update the Transformation
     */
    public final void updateTransformation(AbstractTransformation previousTransformation, SettingsSet settings) {
        isTransformationDone = false;
        outputImage = executeTransformation(previousTransformation, settings);
        settingsValues = settings;
        isTransformationDone = true;
        shouldAbortUpdate = false;
        for (TransformationResultChangeListener listener : listeners) {
            listener.transformationResultChanged(this);
        }
    }

    /**
     * Abort the Transformation update
     */
    public final void abortUpdate() {
        shouldAbortUpdate = true;
    }

    /**
     * Indicate if the Transformation is complete
     * 
     * @return true if the Transformation is complete, false otherwise
     */
    public final boolean isTransformationDone() {
        return isTransformationDone;
    }

    /**
     * Indicate if the output image is vectorized
     * 
     * @return true if the output image is vectorized, false if it is a bitmap
     */
    public final boolean isOutputImageVectorized() {
        return isOutputImageVectorized;
    }

    /**
     * Execute the Transformation
     * 
     * @param previousTransformation the previous Transformation, which this Transformation is then applied on the
     *                               output image
     * @param settings               the SettingsSet containing the settings with which to execute the Transformation
     * @return
     */
    protected abstract Image executeTransformation(AbstractTransformation previousTransformation, SettingsSet settings);

    /**
     * Draw the vectorized image output, if applicable
     * 
     * @param g the Graphics2D object with which to draw the image
     */
    public void drawVectorizedImageOutput(Graphics2D g) {
        /* nothing to do */
    }

}
