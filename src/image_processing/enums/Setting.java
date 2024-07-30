package image_processing.enums;

/**
 * The Configuration available Settings
 * 
 * @author Terence
 *
 */
public enum Setting {

    PEN_TIP_DIAMETER("Pen Tip Diameter"), /* the pen tip diameter, in mm */

    IMAGE_FILE("Source Image"), /* the input image path */
    IMAGE_DPI("Image DPI"), /* the input image DPI */
    COLOR_SPACE("Color Space"), /* the color space to use for all color-related computations */

    NB_QUANTIZATION_COLORS("Number of Colors to keep"), /* the number of color to keep */
    BLURRING_RADIUS("Blurring Radius"), /* the blurring radius used during the color quantization transformation */
    BLURRING_INTENSITY("Blurring Intensity"), /*
                                               * the blurring intensity used during the color quantization
                                               * transformation
                                               */
    COLOR_QUANTIZATION_METHOD("Color Quantization Method"), /* the color quantization method to use */
    COLOR_SELECTION_METHOD("Color Selection Method"), /* the color selection method to use */

    LPMM_MAX("Lines Per Mm Max"), /* the maximum number of lines per mm to draw */
    NB_LEVELS_PER_COLOR("Levels Per Color"), /* the number of different levels allowed per quantized color */
    COLOR_ATTRIBUTION_METHOD("Color Attribution Method"), /* the color attribution method to use */
    GREY_SATURATION_THRESHOLD("Grey Saturation Threshold"), /* the grey saturation threshold to use */
    BRUSH_PALETTES("Brush Palettes"), /* the brush palettes available */
    ID_SELECTED_BRUSH_PALETTE("Selected Brush Palette"), /* the ID of the currently selected brush palette */

    ENABLE_FINE_OUTLINING("Enable Fine Outlining"), /* enable or disable the fine outlining option */
    ENABLE_THICK_OUTLINING("Enable Thick Outlining"), /* enable or disable the thick outlining option */
    MULTICOLOR_THICK_OUTLINING("Enable Multicolor Thick Outlines"), /*
                                                                     * use multicolor thick outlining instead of black
                                                                     * thick outlining
                                                                     */
    OUTLINE_LPMM("Lines Per Mm"), /* the thick outling lines per mm */
    CANNY_HIGH_THRESHOLD("High Threshold"), /* the Canny high threshold value used for the outlining */
    CANNY_LOW_THRESHOLD("Low Threshold"), /* the Canny low threshold value used for the outlining */

    MIN_SEGMENT_LENGTH("Min. Segment Length"), /* the minimum path segment length to keep */
    CLEAR_Z_HEIGHT("Clear Z Height"), /*
                                       * the height in mm at which to move to perform clear (i.e. non-drawing) movements
                                       */
    OUTPUT_POSITION("Position"), /* the output position within an A4 sheet */
    OUTPUT_ROTATION("Rotation"), /* the output rotation option */
    OUTPUT_MIRRORING("Mirroring"); /* the output mirroring option */

    /**
     * The human-friendly setting label
     */
    private String label;

    /**
     * Instantiate a new Setting
     * 
     * @param label the Setting human-friendly label
     */
    private Setting(String label) {
        this.label = label;
    }

    /**
     * Get the Setting human-friendly label
     * 
     * @return the Setting human-friendly label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Get the Setting human-friendly name
     * 
     * @return the Setting human-friendly name
     */
    public String getName() {
        return name().toLowerCase();
    }

}
