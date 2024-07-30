package image_processing.transformations;

import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

import common.engine.Ink;
import common.engine.Line;
import common.engine.Path;
import common.engine.SettingsSet;
import common.engine.Utils;
import image_processing.engine.Brush;
import image_processing.engine.BrushPalette;
import image_processing.engine.Image;
import image_processing.engine.canny.ImageUtils;
import image_processing.enums.Setting;
import image_processing.generators.ImageFineOutliner;
import image_processing.session.Project;

/**
 * Outline an image
 * 
 * @author Terence
 *
 */
public class FineOutliningTransformation extends AbstractTransformation {

    /**
     * The outline paths, in px
     */
    private Vector<Path> outlinePaths = new Vector<Path>();

    /**
     * Instantiate a Fine Outlining Transformation
     */
    public FineOutliningTransformation() {
        super(TransformationStep.FINE_OUTLINING,
                new Setting[] { Setting.ENABLE_FINE_OUTLINING, Setting.PEN_TIP_DIAMETER }, true);
    }

    /**
     * Get the outline paths, with units in px
     * 
     * @return the outline paths, with units in px
     */
    public Vector<Path> getOutlinePaths() {
        return outlinePaths;
    }

    @Override
    public void drawVectorizedImageOutput(Graphics2D g) {
        /* Draw the thick outlining transformation output image */
        g.setColor(Ink.getBlackestAvailableInk().getColor());
        Image thickOutlineImg = Project.Instance.getTransformation(TransformationStep.THICK_OUTLINING).getOutputImage();
        if (thickOutlineImg != null) {
            g.drawImage(thickOutlineImg.getBufferedImage(), 0, 0, null);
        }

        /* Draw the fine outlining paths */
        Stroke defaultStroke = g.getStroke();
        g.setStroke(Utils.getPenStrokeInMm(settingsValues.getDoubleSetting(Setting.PEN_TIP_DIAMETER)));
        for (Path path : outlinePaths) {
            for (Line line : path.getLines()) {
                g.draw(new Line2D.Double(line.x0, line.y0, line.x1, line.y1));
            }
        }
        g.setStroke(defaultStroke);
    }

    @Override
    protected Image executeTransformation(AbstractTransformation previousTransformation, SettingsSet settings) {
        ColorQuantizationTransformation cqt = (ColorQuantizationTransformation) Project.Instance
                .getTransformation(TransformationStep.COLOR_QUANTIZATION);
        if (settings.getBoolSetting(Setting.ENABLE_FINE_OUTLINING)) {
            /* The fine outlining is enabled, compute it */
            HashMap<Integer, BufferedImage> monochromesImagesPerColor = cqt.getMonochromesImagesPerColor();
            BrushPalette brushPalette = settings.getSelectedBrushPalette();

            /* Merge all the monochromes images for which the fine outlining is enabled into a single one */
            Vector<BufferedImage> imagesToOutline = new Vector<BufferedImage>();
            for (Entry<Integer, BufferedImage> entry : monochromesImagesPerColor.entrySet()) {
                int color = entry.getKey();
                Brush brush = brushPalette.getBrushByInputColor(color);
                if (brush != null && brush.isFineOutliningEnabled()) {
                    imagesToOutline.add(entry.getValue());
                }
            }
            BufferedImage img = ImageUtils.mergeImages(imagesToOutline.toArray(new BufferedImage[0]), 0x00000000);

            /* Compute the fine outline on the merged image */
            outlinePaths = ImageFineOutliner.generateImageFineOutlinePaths(img);
            int W = previousTransformation.getOutputImage().getWidth();
            int H = previousTransformation.getOutputImage().getHeight();
            BufferedImage outlineImg = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            drawVectorizedImageOutput((Graphics2D) outlineImg.getGraphics());

            /* Merge the thick and fine outlining results into a single image */
            BufferedImage mergedThickAndFineOutlinings = ImageUtils.mergeImages(
                    new BufferedImage[] { outlineImg, previousTransformation.getOutputImage().getBufferedImage() },
                    0x00000000);

            return new Image(mergedThickAndFineOutlinings);
        } else {
            /* The fine outlining is disabled, return the previous transformation output image */
            outlinePaths.clear();
            return previousTransformation.getOutputImage();
        }
    }
}
