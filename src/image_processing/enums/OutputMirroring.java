package image_processing.enums;

/**
 * The available output mirrorings
 * 
 * @author Terence
 *
 */
public enum OutputMirroring {

    NO_MIRRORING(0b000, "None"), X_MIRRORING(0b001, "X"), Y_MIRRORING(0b010, "Y"), XY_MIRRORING(0b011, "X + Y");

    /**
     * The mirroring bitfield
     */
    private int mirroring;

    /**
     * The mirroring human-friendly label
     */
    private String label;

    /**
     * Instantiate a new output mirroring type
     * 
     * @param mirroring the mirroring bitfield
     * @param label     the mirroring human-friendly label
     */
    private OutputMirroring(int mirroring, String label) {
        this.mirroring = mirroring;
        this.label = label;
    }

    /**
     * Indicate if the mirroring contains an X mirroring
     * 
     * @return true if the mirroring contains an X mirroring, false otherwise
     */
    public boolean xMirroring() {
        return (mirroring & 0b001) != 0;
    }

    /**
     * Indicate if the mirroring contains an Y mirroring
     * 
     * @return true if the mirroring contains an Y mirroring, false otherwise
     */
    public boolean yMirroring() {
        return (mirroring & 0b010) != 0;
    }

    /**
     * Indicate if the mirroring contains an Z mirroring
     * 
     * @return true if the mirroring contains an Z mirroring, false otherwise
     */
    public boolean zMirroring() {
        return (mirroring & 0b100) != 0;
    }

    @Override
    public String toString() {
        return label;
    }
}
