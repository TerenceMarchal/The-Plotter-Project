package image_processing.enums;

/**
 * The available output rotations
 * 
 * @author Terence
 *
 */
public enum OutputRotation {

    NO_ROTATION(0, "None"), CLOCKWISE_90(-90, "Clockwise 90°"), COUNTER_CLOCKWISE_90(90, "Counter-clockwise 90°"),
    FULL_180(180, "180°");

    /**
     * The rotation angle, in degrees
     */
    private int angle;

    /**
     * The human-friendly rotation label
     */
    private String label;

    /**
     * Instantiate a new output rotation type
     * 
     * @param angle the rotation angle in degrees
     * @param label human-friendly rotation label
     */
    private OutputRotation(int angle, String label) {
        this.angle = angle;
        this.label = label;
    }

    /**
     * Get the rotation angle, in degrees
     * 
     * @return the rotation angle, in degrees
     */
    public int getAngle() {
        return angle;
    }

    @Override
    public String toString() {
        return label;
    }

}
