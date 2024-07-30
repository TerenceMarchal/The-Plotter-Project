package image_processing.gui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import common.engine.Zoom;
import common.gui.View;
import image_processing.engine.Image;
import image_processing.listeners.TransformationResultChangeListener;
import image_processing.session.Project;
import image_processing.transformations.AbstractTransformation;
import image_processing.transformations.TransformationStep;

/**
 * A View to display the result of a Transformation
 * 
 * @author Terence
 *
 */
public class TransformationResultView extends View implements TransformationResultChangeListener {

    /**
     * The Zoom to share accorss all the Transformation Result Views
     */
    private static final Zoom IMAGE_PROCESSING_VIEW_ZOOM = new Zoom();

    /**
     * The Transformation of which to display the result
     */
    private final AbstractTransformation transformation;

    /**
     * Instantiate a new Transformation Result View
     * 
     * @param title          the View title
     * @param transformation the corresponding Transformation to display
     */
    public TransformationResultView(String title, AbstractTransformation transformation) {
        super(title, IMAGE_PROCESSING_VIEW_ZOOM);
        this.transformation = transformation;
        transformation.addListener(this);
    }

    @Override
    protected void paint(Graphics2D g, int W, int H) {
        Image originalImage = Project.Instance.getTransformation(TransformationStep.IMAGE_IMPORT).getOutputImage();
        if (originalImage != null) {
            int trX = (W - originalImage.getWidth()) / 2;
            int trY = (H - originalImage.getHeight()) / 2;
            g.translate(trX, trY);
            if (!transformation.isOutputImageVectorized()) {
                if (transformation.getOutputImage() != null) {
                    BufferedImage img = transformation.getOutputImage().getBufferedImage();
                    if (img != null) {
                        g.drawImage(img, 0, 0, null);
                    }
                }
            } else {
                transformation.drawVectorizedImageOutput(g);
            }
            g.translate(-trX, -trY);
        }
    }

    @Override
    public void transformationResultChanged(AbstractTransformation transformation) {
        repaint();
    }

    @Override
    public void transformationProgressionChanged(AbstractTransformation transformation, String progressionLabel,
            double progression, boolean shouldRepaint) {
        if (shouldRepaint) {
            repaint();
        }
    }

}
