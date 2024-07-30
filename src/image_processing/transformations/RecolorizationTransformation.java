package image_processing.transformations;

import common.engine.SettingsSet;
import image_processing.engine.BrushPalette;
import image_processing.engine.Image;
import image_processing.enums.Setting;
import image_processing.generators.BrushPaletteGenerator;
import image_processing.generators.ImageGenerator;
import image_processing.session.Configuration;

/**
 * A Transformation to re-colorize an image
 * 
 * @author Terence
 *
 */
public class RecolorizationTransformation extends AbstractTransformation {

    /**
     * Instantiate a Re-colorization Transformation
     */
    public RecolorizationTransformation() {
        super(TransformationStep.RECOLORIZATION,
                new Setting[] { Setting.BRUSH_PALETTES, Setting.ID_SELECTED_BRUSH_PALETTE }, false);
    }

    @Override
    protected Image executeTransformation(AbstractTransformation previousTransformation, SettingsSet settings) {
        /* Retrieve the BrushPalette to use for the re-colorization */
        BrushPalette brushPalette = settings.getSelectedBrushPalette();

        /* If needed, regenerate the BrushPalette */
        if (brushPalette.getNbBrushes() == 0) {
            brushPalette = BrushPaletteGenerator.generateBrushPalette(previousTransformation.getOutputColorPalette(),
                    settings.getDoubleSetting(Setting.LPMM_MAX), settings.getIntSetting(Setting.NB_LEVELS_PER_COLOR),
                    settings.getDoubleSetting(Setting.GREY_SATURATION_THRESHOLD),
                    settings.getColorSpaceSetting(Setting.COLOR_SPACE),
                    settings.getColorAttributionMethodSetting(Setting.COLOR_ATTRIBUTION_METHOD));
            Configuration.Instance.setArraySettingValue(Setting.BRUSH_PALETTES,
                    settings.getIntSetting(Setting.ID_SELECTED_BRUSH_PALETTE), brushPalette.toJSonObject());
        }

        Image outputImage = ImageGenerator.generateImagePaintedWithBrushPalette(previousTransformation.getOutputImage(),
                brushPalette);
        return outputImage;
    }

}