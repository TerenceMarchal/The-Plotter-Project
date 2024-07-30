package image_processing.enums;

/**
 * The available output positions
 * 
 * @author Terence
 *
 */
public enum OutputPosition {
    TOP_LEFT_CORNER(0, 0, "Top-left corner"), CENTERED_IN_A4(0.5, 0.5, "Centered in A4 sheet"),
    CENTERED_IN_UPPER_HALF_A4(0.5, 0.75, "Centered in A4 sheet upper half"),
    CENTERED_IN_LOWER_HALF_A4(0.5, 0.25, "Centered in A4 sheet lower half"),
    CENTERED_IN_UPPER_LEFT_A4(0.25, 0.75, "Centered in A4 sheet upper left quarter"),
    CENTERED_IN_UPPER_RIGHT_A4(0.75, 0.75, "Centered in A4 sheet upper right quarter"),
    CENTERED_IN_LOWER_LEFT_A4(0.25, 0.25, "Centered in A4 sheet lower left quarter"),
    CENTERED_IN_LOWER_RIGHT_A4(0.75, 0.25, "Centered in A4 sheet lower right quarter");

    /**
     * The width in mm of an A4 sheet
     */
    private static final int A4_WIDTH = 210;

    /**
     * The height in mm of an A4 sheet
     */
    private static final int A4_HEIGHT = 297;

    /**
     * The output position offset in an A4 sheet
     */
    private double xPosInA4, yPosInA4;

    /**
     * The human-friendly position label
     */
    private String label;

    /**
     * Instantiate a new output position
     * 
     * @param xRelativePos the output X relative position, between 0.0 and 1.0
     * @param yRelativePos the output Y relative position, between 0.0 and 1.0
     * @param label        the human-friendly position label
     */
    private OutputPosition(double xRelativePos, double yRelativePos, String label) {
        this.xPosInA4 = xPosInA4 * A4_WIDTH;
        this.yPosInA4 = yPosInA4 * A4_HEIGHT;
        this.label = label;
    }

    /**
     * Get the output X position in mm ib an A4 sheet
     * 
     * @return the output X position in mm ib an A4 sheet
     */
    public double getXPosInA4() {
        return xPosInA4;
    }

    /**
     * Get the output Y position in mm ib an A4 sheet
     * 
     * @return the output Y position in mm ib an A4 sheet
     */
    public double getYPosInA4() {
        return yPosInA4;
    }

    /**
     * Indicate if the output should be center around its position
     * 
     * @return true if the output should be center around its position, false if it should be considered its top-left
     *         origin
     */
    public boolean isCentered() {
        return this != TOP_LEFT_CORNER;
    }

    @Override
    public String toString() {
        return label;
    }
}
