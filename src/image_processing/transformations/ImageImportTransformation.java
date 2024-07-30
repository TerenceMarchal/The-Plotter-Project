package image_processing.transformations;

import common.engine.SettingsSet;
import image_processing.engine.Image;
import image_processing.enums.Setting;

/**
 * A Transformation that simply import an image from a file
 * 
 * @author Terence
 *
 */
public class ImageImportTransformation extends AbstractTransformation {

    /**
     * Instantiate an Image Import Transformation
     */
    public ImageImportTransformation() {
        super(TransformationStep.IMAGE_IMPORT, new Setting[] { Setting.IMAGE_FILE }, false);

    }

    @Override
    protected Image executeTransformation(AbstractTransformation previousTransformation, SettingsSet settings) {
        return new Image(settings.getFileSetting(Setting.IMAGE_FILE));
    }

}
