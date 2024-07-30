package image_processing.generators;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import image_processing.engine.Brush;
import image_processing.engine.BrushPalette;
import image_processing.engine.ColorPalette;
import image_processing.engine.Image;
import image_processing.enums.ColorSpace;

/**
 * An abstract class to generate Image
 * 
 * @author Terence
 *
 */
public abstract class ImageGenerator {

    /**
     * Generate a blurred image from the source image
     * 
     * @param sourceImg the image from which to generate the blurred image
     * @param radius    the blurring radius
     * @param intensity the blurring intensity
     * @return the generated blurred image
     */
    public static Image generateBlurredImage(Image sourceImg, int radius, double intensity) {
        int W = sourceImg.getWidth();
        int H = sourceImg.getHeight();
        BufferedImage bufferedImg = sourceImg.getBufferedImage();

        /* Create the blurred image */
        BufferedImage blurredImg = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics g = blurredImg.getGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, W, H);

        /* Compute costly constants */
        double x2SquaredIntensity = 2 * intensity * intensity;
        double invSqrt2PiXIntensity = 1 / (Math.sqrt(2 * Math.PI) * intensity);

        /* Compute the Gaussian kernel */
        double norm = 0;
        double[] mask = new double[2 * radius + 1];
        for (int x = -radius; x < radius + 1; x++) {
            double exp = Math.exp(-((x * x) / x2SquaredIntensity));

            mask[x + radius] = invSqrt2PiXIntensity * exp;
            norm += mask[x + radius];
        }

        /* Convolve the image with the Gaussian kernel horizontally and vertically */
        for (int axis = 0; axis < 2; axis++) {
            boolean convoleHorizontally = axis == 0;
            for (int r = radius; r < H - radius; r++) {
                for (int c = radius; c < W - radius; c++) {
                    double[] sum = new double[3];

                    for (int mr = -radius; mr < radius + 1; mr++) {
                        for (int chan = 0; chan < 3; chan++) {
                            int raw = (bufferedImg.getRGB(c + (convoleHorizontally ? mr : 0),
                                    r + (convoleHorizontally ? 0 : mr)) & (0x0000FF << (8 * chan))) >> (8 * chan);
                            sum[chan] += (mask[mr + radius] * raw);
                        }
                    }

                    /* Normalize the RGB channels */
                    int color = 0xFF000000;
                    for (int chan = 0; chan < 3; chan++) {
                        sum[chan] /= norm;
                        color |= ((int) Math.round(sum[chan])) << (8 * chan);
                    }
                    blurredImg.setRGB(c, r, color);
                }
            }
        }

        return new Image(blurredImg);
    }

    /**
     * Generate an Image recolored with the specified color palette
     * 
     * @param sourceImg                   the source image to recolor
     * @param colorPalette                the color palette to use to generate the recolored image
     * @param monochromesPicturesPerColor an HashMap to fill with a monochrome image for every color from the color
     *                                    palette
     * @param ColorSpace                  the color space to use to generate the recolored image
     * @return the generated image
     */
    public static Image generateRecoloredImage(Image sourceImg, ColorPalette colorPalette,
            HashMap<Integer, BufferedImage> monochromesPicturesPerColor, ColorSpace colorSpace) {
        int W = sourceImg.getWidth();
        int H = sourceImg.getHeight();
        BufferedImage recoloredBuffImg = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);

        monochromesPicturesPerColor.clear();
        for (int color : colorPalette.getColors()) {
            monochromesPicturesPerColor.put(color, new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB));
        }

        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                int color = colorPalette.getClosestColor(sourceImg.getPixel(x, y), colorSpace);
                recoloredBuffImg.setRGB(x, y, 0xFF000000 | color);
                BufferedImage monochromePicture = monochromesPicturesPerColor.get(color);
                if (monochromePicture != null) {
                    monochromePicture.setRGB(x, y, 0xFF000000 | color);
                }
            }
        }
        return new Image(recoloredBuffImg);
    }

    /**
     * Generate a new image from the specified source image by applying the specified BrushPalette
     * 
     * Every pixel from the source image is replaced in the generated image by the corresponding aimed color of the
     * BrushPalette
     * 
     * @param sourceImg    the original image
     * @param brushPalette the BrushPalette with which to generate the new one
     * @return the generated image
     */
    public static Image generateImagePaintedWithBrushPalette(Image sourceImg, BrushPalette brushPalette) {
        int W = sourceImg.getWidth();
        int H = sourceImg.getHeight();
        BufferedImage paintedBuffImg = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = paintedBuffImg.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, W, H);

        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                g.setColor(Color.white);
                for (Brush brush : brushPalette.getBrushes()) {
                    if ((sourceImg.getPixel(x, y) & 0xFFFFFF) == brush.getInputColor()) {
                        g.setColor(new Color(brush.getAimedOutputColor()));
                        break;
                    }
                }
                g.fillRect(x, y, 1, 1);
            }
        }
        return new Image(paintedBuffImg);
    }

}
