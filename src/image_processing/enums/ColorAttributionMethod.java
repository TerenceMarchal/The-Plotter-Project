package image_processing.enums;

/**
 * The available color attribution methods
 * 
 * @author Terence
 *
 */
public enum ColorAttributionMethod {
    ASSIGN_CLOSEST_COLOR, /* assign the closest available color from the original color */
    FAVOR_MORE_CONTRASTS, /*
                           * use a widest distribution of colors, favoring more contrast instead of colors closer to the
                           * originals
                           */
    ASSIGN_CLOSEST_QUANTIZED_INK, /* use the inks colors available */
}
