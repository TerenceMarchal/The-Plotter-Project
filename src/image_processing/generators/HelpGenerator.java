package image_processing.generators;

import java.util.Locale;

import javax.swing.JCheckBox;
import javax.swing.JLabel;

import image_processing.enums.Setting;
import image_processing.gui.JComponentWithHelp;

/**
 * An abstract class that generate help and documentation
 * 
 * @author Terence
 *
 */
public abstract class HelpGenerator {

    /**
     * Get an HTML description of the specified Setting
     * 
     * @param setting the Setting for which to retrieve the HTML description
     * @return the HTML description of the specified Setting
     */
    public static String getSettingHtmlDescription(Setting setting) {
        String description = "<i>No description available</i>";

        switch (setting) {
        case PEN_TIP_DIAMETER:
            description = "The tip's diameter of the pen used by the Plotter, in mm";
            break;
        case IMAGE_DPI:
            description = "The source image resolution, in Dots Per Inches\n\n"
                    + "<i>image_dpi = image_width_in_pixels / image_width_in_mm * 25.4</i>";
            break;
        case COLOR_SPACE:
            description = "The color space used for all color-related computation\n\n"
                    + "<b>RGB</b>: the default Red-Green-Blue color space\n"
                    + "<b>sRGB</b>: the standard Red-Green-Blue color space, widely used by monitors, printer and cameras\n"
                    + "<b>Human-Weighted</b>: a weighted version of the RGB color space, to better reflect the human eye sensitivity\n\n"
                    + "<i>The easiest thing to do is to try the different color spaces and keep the one that best fit your source image.</i>";
            break;
        case NB_QUANTIZATION_COLORS:
            description = "The number of colors to keep at the end of the color-quantization processus";
            break;
        case BLURRING_RADIUS:
            description = "Blurring the color-quantized image before re-coloring it help reducing local spots of different colors\n\n"
                    + "<i>Increasing the radius help get rid of more spots, but it is slower and some details are lost</i>";
            break;
        case BLURRING_INTENSITY:
            description = "Blurring the color-quantized image before re-coloring it help reducing local spots of different colors\n\n"
                    + "<i>Increasing the intensity decrease the weigth of the central pixel during the blur computation</i>";
            break;
        case COLOR_QUANTIZATION_METHOD:
            description = "The color quantization method to use to select the colors used for the image re-colorization\n\n"
                    + "<b>Use Available Inks</b>: simply use the inks color available to the Plotter, without any computation\n"
                    + "<b>Favor Contrasts</b>: use a variant of the Bucket-Cutting algorithm that favor large contrasts between the selected colors\n"
                    + "<b>Favor Colors True to the Originals</b>: use the classic Bucket-Cutting algorithm that favor a reduced set of colors true to the original colors";
            break;
        case COLOR_SELECTION_METHOD:
            description = "The color selection method used to compute the quantized color from all the original regrouped colors\n\n"
                    + "<b>Median</b>: use the median color, minimizing the difference between all the original colors and the quantized one\n"
                    + "<b>Average</b>: average all the original colors to compute the quantized one";
            break;
        case LPMM_MAX:
            description = "The maximum number of lines to draw per mm";
            break;
        case NB_LEVELS_PER_COLOR:
            description = "The number of filling levels to allow per quantized color (not counting the 0% filling level)\n\n"
                    + "<i>With a filling level of 0%, no lines will be drawn; with a filling level of 100%, the specified maximum lines will be drawn per mm</i>";

            break;
        case COLOR_ATTRIBUTION_METHOD:
            description = "The color attribution method to use to generate the Brush Palette used to re-colorize the picture with the quantified colors and filling levels\n\n"
                    + "<b>Assign Closest Color</b>: use the closest color available, generating a drawing as true to the original as possible\n"
                    + "<b>Favor More Contrasts</b>: use as much different colors as possible, generating a drawing with more contrasts but less true to the original\n"
                    + "<b>Assign Closest Quantized Ink</b>: use the available inks colors with different filling levels";
            break;
        case GREY_SATURATION_THRESHOLD:
            description = "The saturation threshold under which a color is considered to be a grey color (drawn using black ink) instead of a non-grey color (drawn using any of the other inks)";
            break;
        case ENABLE_FINE_OUTLINING:
            description = "Enable or disable the fine outlining option, drawn as a black outline path";
            break;
        case ENABLE_THICK_OUTLINING:
            description = "Enable or disable the thick outlining option, that is then drawn with the specified lines per mm";
            break;
        case MULTICOLOR_THICK_OUTLINING:
            description = "Enable or disable the multicolor option for the thick outlines, which will be drawn according to their own ink color or always with black ink";
            break;
        case OUTLINE_LPMM:
            description = "The number of lines to draw per mm for thick outlines";
            break;
        case CANNY_HIGH_THRESHOLD:
            description = "The high threshold of the Canny algorithm used for the thick outlines\n\n"
                    + "<i>The easiest thing to do is changing the high and low thresholds until you are pleased with the results.</i>";
            break;
        case CANNY_LOW_THRESHOLD:
            description = "The low threshold of the Canny algorithm used for the thick outlines\n\n"
                    + "<i>The easiest thing to do is changing the high and low thresholds until you are pleased with the results.</i>";
            break;
        case MIN_SEGMENT_LENGTH:
            description = "The minimum length in mm of a drawing or flying motion\n\n"
                    + "<i>Increasing this value leads to drawings less precise but can drastically shorten its duration by reducing the up-and-down motions of the Plotter</i>";
            break;
        case CLEAR_Z_HEIGHT:
            description = "The height in mm at which to move to perform non-drawing (flying) motions";
            break;
        case OUTPUT_POSITION:
            description = "The drawing position within an A4 sheet, obtained by offsetting the G-Code instructions";
            break;
        case OUTPUT_ROTATION:
            description = "The drawing rotation";
            break;
        case OUTPUT_MIRRORING:
            description = "The drawing mirroring, useful for Plotter whose axis don't follow the 3-fingers rule";
            break;

        default:
            System.out.println("No HTML description available for setting " + setting.name());
            break;
        }
        return String.format(Locale.US, "<html>%s</html>", description.replaceAll("\n", "<br/>"));
    }

    /**
     * Create a JComponentWithHelp with a JLabel corresponding to the specified Setting
     * 
     * @param setting the Setting for which to generate the JComponentWithHelp
     * @return the generated JComponentWithHelp
     */
    public static JComponentWithHelp getSettingLabelWithHelp(Setting setting) {
        return new JComponentWithHelp(new JLabel(setting.getLabel() + ":"),
                HelpGenerator.getSettingHtmlDescription(setting));
    }

    /**
     * Create a JComponentWithHelp with a JCheckBox corresponding to the specified Setting
     * 
     * @param setting the Setting for which to generate the JComponentWithHelp
     * @return the generated JComponentWithHelp
     */
    public static JComponentWithHelp getSettingCheckboxWithHelp(Setting setting) {
        return new JComponentWithHelp(new JCheckBox(setting.getLabel()),
                HelpGenerator.getSettingHtmlDescription(setting));
    }

}
