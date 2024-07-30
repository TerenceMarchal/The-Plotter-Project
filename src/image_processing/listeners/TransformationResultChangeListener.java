package image_processing.listeners;

import image_processing.transformations.AbstractTransformation;

/**
 * Listener to be notified when a Transformation result change
 * 
 * TODO-014: try to merge this Listener with the ComputationProgressionListener
 * 
 * @author Terence
 *
 */
public interface TransformationResultChangeListener {

    /**
     * Callback called when the result of a Transformation changed
     * 
     * @param transformation the Transformation of which the result changed
     */
    public void transformationResultChanged(AbstractTransformation transformation);

    /**
     * Callback called when a Transformation progression changed
     * 
     * @param transformation   the Transformation of which the progression changed
     * @param progressionLabel a human-friendly label indicating the current Transformation progression status
     * @param progression      the current Transformation progression, between 0.0 and 1.0
     * @param shouldRepaint    indicate if a new intermediate Transformation output can be repainted
     */
    public void transformationProgressionChanged(AbstractTransformation transformation, String progressionLabel,
            double progression, boolean shouldRepaint);

}
