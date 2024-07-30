package image_processing.transformations;

/**
 * A Transformation Step
 * 
 * @author Terence
 *
 */
public enum TransformationStep {
    IMAGE_IMPORT("Image import"), COLOR_QUANTIZATION("Color Quantization"), RECOLORIZATION("Recolorization"),
    THICK_OUTLINING("Thick outlining"), FINE_OUTLINING("Outlining"), PATHS_GENERATION("Paths generation"),
    PATHS_OPTIMIZATION("Paths optimization"), THINNING("Thinning");

    /**
     * The Transformation step name
     */
    private String name;

    /**
     * Instantiate a Transformation step
     * 
     * @param name the Transformation step nale
     */
    private TransformationStep(String name) {
        this.name = name;
    }

    /**
     * Get the step index
     * 
     * @return the step index
     */
    public int getStep() {
        return ordinal() + 1;
    }

    /**
     * Get the total number of steps
     * 
     * TODO-024: check that it is still used properly with the new thinning transformation
     * 
     * @return the total number of steps
     */
    public int getNbSteps() {
        return values().length;
    }

    /**
     * Get the Transformation step name
     * 
     * @return the Transformation step name
     */
    public String getName() {
        return name;
    }
}
