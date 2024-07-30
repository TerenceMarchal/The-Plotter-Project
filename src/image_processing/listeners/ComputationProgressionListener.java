package image_processing.listeners;

import image_processing.transformations.AbstractTransformation;

/**
 * A Listener to be notified when the computation progression change
 * 
 * @author Terence
 *
 */
public interface ComputationProgressionListener {

    /**
     * Callback called when the Transformation being computed change
     * 
     * @param currentTransformation the Transformation now being computed
     */
    public void currentTransformationChanged(AbstractTransformation currentTransformation);

    /**
     * Callback called when the progression on the Transformation being computed change
     * 
     * @param currentTransformation            the Transformation being computed
     * @param progressionLabel                 a human-friendly label of the current computation status
     * @param currentTransformationProgression the Transformation's computation current progression, between 0.0 and 1.0
     */
    public void currentTransformationProgressionChanged(AbstractTransformation currentTransformation,
            String progressionLabel, double currentTransformationProgression);

}
