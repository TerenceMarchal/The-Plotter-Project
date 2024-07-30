package image_processing.transformations;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

import common.engine.Ink;
import common.engine.SettingsSet;
import image_processing.engine.Brush;
import image_processing.engine.BrushPalette;
import image_processing.engine.Image;
import image_processing.engine.canny.ImageUtils;
import image_processing.engine.canny.JCanny;
import image_processing.enums.Setting;
import image_processing.session.Project;

/**
 * A Transformation to add a thick outline on an image
 * 
 * It means that the outlining is added to the image and processed as if it were part of the original image, i.e.
 * parameters such as lines per mm will have an impact on it
 * 
 * @author Terence
 *
 */
public class ThickOutliningTransformation extends AbstractTransformation {

    /**
     * Instantiate a Thick Outlining Transformation
     */
    public ThickOutliningTransformation() {
        super(TransformationStep.THICK_OUTLINING, new Setting[] { Setting.ENABLE_THICK_OUTLINING,
                Setting.MULTICOLOR_THICK_OUTLINING, Setting.CANNY_HIGH_THRESHOLD, Setting.CANNY_LOW_THRESHOLD }, false);
    }

    @Override
    protected Image executeTransformation(AbstractTransformation previousTransformation, SettingsSet settings) {
        ColorQuantizationTransformation cqt = (ColorQuantizationTransformation) Project.Instance
                .getTransformation(TransformationStep.COLOR_QUANTIZATION);
        if (settings.getBoolSetting(Setting.ENABLE_THICK_OUTLINING)) {
            /* The thick outlining is enabled, compute it */
            BrushPalette brushPalette = settings.getSelectedBrushPalette();
            boolean multicolor = settings.getBoolSetting(Setting.MULTICOLOR_THICK_OUTLINING);

            /* For each monochrome image of the Color Quantization Transformation, compute the thick outline */
            HashMap<Integer, BufferedImage> monochromesImagesPerColor = cqt.getMonochromesImagesPerColor();
            Vector<BufferedImage> outlinesImages = new Vector<BufferedImage>();
            for (Entry<Integer, BufferedImage> entry : monochromesImagesPerColor.entrySet()) {
                Brush brush = brushPalette.getBrushByInputColor(entry.getKey());
                if (brush != null && brush.isThickOutliningEnabled() && brush.getInk() != null) {
                    int outlineColor = multicolor ? brush.getInk().getColorAsRgb()
                            : Ink.getBlackestAvailableInk().getColorAsRgb();
                    // TODO-049: try to get rid of the canny package
                    outlinesImages.add(
                            JCanny.CannyEdges(entry.getValue(), settings.getIntSetting(Setting.CANNY_HIGH_THRESHOLD),
                                    settings.getDoubleSetting(Setting.CANNY_LOW_THRESHOLD), outlineColor));
                }
            }

            /* Merge all the outlines in a single image */
            return new Image(ImageUtils.mergeImages(outlinesImages.toArray(new BufferedImage[0]), 0xFFFFFF));
        } else {
            /* The thick outlining is disabled, generate a blank image */
            BufferedImage img = new BufferedImage(cqt.getOutputImage().getWidth(), cqt.getOutputImage().getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics g = img.getGraphics();
            g.setColor(Color.white);
            g.fillRect(0, 0, img.getWidth(), img.getHeight());
            return new Image(img);
        }
    }
}
