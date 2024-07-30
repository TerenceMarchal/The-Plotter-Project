package image_processing.transformations;

import java.awt.image.BufferedImage;
import java.util.HashMap;

import common.engine.SettingsSet;
import image_processing.engine.Image;
import image_processing.enums.ColorQuantizationMethod;
import image_processing.enums.ColorSelectionMethod;
import image_processing.enums.Setting;
import image_processing.generators.BrushPaletteGenerator;
import image_processing.generators.ColorPaletteGenerator;
import image_processing.generators.ImageGenerator;

/**
 * Apply a color quantization on an image, i.e. reduce their number
 * 
 * @author Terence
 *
 */
public class ColorQuantizationTransformation extends AbstractTransformation {

    /**
     * An HashMap containing the quantized colors and their corresponding monochromes images
     */
    private HashMap<Integer, BufferedImage> monochromesImagesPerColor = new HashMap<Integer, BufferedImage>();

    /**
     * Instantiate a Color Quantization Transformation
     */
    public ColorQuantizationTransformation() {
        super(TransformationStep.COLOR_QUANTIZATION,
                new Setting[] { Setting.COLOR_SPACE, Setting.NB_QUANTIZATION_COLORS, Setting.BLURRING_RADIUS,
                        Setting.BLURRING_INTENSITY, Setting.COLOR_QUANTIZATION_METHOD, Setting.COLOR_SELECTION_METHOD },
                false);
    }

    /**
     * Get the monochromes images per quantized color corresponding to the Transformation
     * 
     * @return an HashMap containing the quantized colors and their corresponding monochromes images
     */
    public HashMap<Integer, BufferedImage> getMonochromesImagesPerColor() {
        return monochromesImagesPerColor;
    }

    @Override
    public Image executeTransformation(AbstractTransformation previousTransformation, SettingsSet settings) {
        Image inputImage = previousTransformation.getOutputImage();

        /* Generate the output color palette */
        if (settings.getColorQuantizationMethodSetting(
                Setting.COLOR_QUANTIZATION_METHOD) == ColorQuantizationMethod.USE_AVAILABLE_INKS) {
            outputColorPalette = BrushPaletteGenerator
                    .generateBrushPaletteFromInks(settings.getIntSetting(Setting.NB_QUANTIZATION_COLORS))
                    .getInputColorPalette();
        } else {
            outputColorPalette = ColorPaletteGenerator.generateReducedPaletteColorFromImage(inputImage,
                    settings.getIntSetting(Setting.NB_QUANTIZATION_COLORS),
                    ColorQuantizationMethod.values()[settings.getIntSetting(Setting.COLOR_QUANTIZATION_METHOD)],
                    ColorSelectionMethod.values()[settings.getIntSetting(Setting.COLOR_SELECTION_METHOD)]);
        }

        /* Blur the image if needed, useful to get rid of small area of different colors */
        Image blurredImage = ImageGenerator.generateBlurredImage(inputImage,
                settings.getIntSetting(Setting.BLURRING_RADIUS), settings.getDoubleSetting(Setting.BLURRING_INTENSITY));

        /* Re-color the image with the quantized color palette we just generated */
        Image recoloredImage = ImageGenerator.generateRecoloredImage(blurredImage, outputColorPalette,
                monochromesImagesPerColor, settings.getColorSpaceSetting(Setting.COLOR_SPACE));

        return recoloredImage;
    }

}
