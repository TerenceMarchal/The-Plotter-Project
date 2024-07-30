package image_processing.enums;

/**
 * The available color quantization methods
 * 
 * @author Terence
 *
 */
public enum ColorQuantizationMethod {
    USE_AVAILABLE_INKS, /* use the available inks colors */
    FAVOR_CONTRASTS, /* use the bucket cutting method, favoring the largest color delta */
    FAVOR_COLORS_TRUE_TO_ORIGINALS, /* use the bucket cutting method with the classic recursive cut */
}
