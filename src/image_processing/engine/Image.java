package image_processing.engine;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * A class representing a raw image
 * 
 * @author Terence
 *
 */
public class Image {

    /**
     * The BufferedImage corresponding to the image
     */
    private final BufferedImage bufferedImg;

    /**
     * A 2D array of the image pixels
     * 
     * TODO-050: try to get rid of this array
     */
    private final int[][] pixels;

    /**
     * Instantiate a new image from a system file
     * 
     * @param file the file from which to load the image
     */
    public Image(File file) {
        int W = 0, H = 0;
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        try {
            img = ImageIO.read(file);
            W = img.getWidth();
            H = img.getHeight();

        } catch (IOException e) {
            System.err.println("Cannot open " + file.getAbsolutePath());
        }
        bufferedImg = img;
        pixels = new int[W][H];
        for (int x = 0; x < W; x++) {
            bufferedImg.getRGB(x, 0, 1, H, pixels[x], 0, 1);
        }
    }

    /**
     * Instantiate a new image directly from a BufferedImage
     * 
     * @param bufferedImg the BufferedImage from which to load the image
     */
    public Image(BufferedImage bufferedImg) {
        this.bufferedImg = bufferedImg;
        if (bufferedImg != null) {
            int W = bufferedImg.getWidth();
            int H = bufferedImg.getHeight();

            pixels = new int[W][H];
            for (int x = 0; x < W; x++) {
                bufferedImg.getRGB(x, 0, 1, H, pixels[x], 0, 1);
            }
        } else {
            pixels = new int[0][0];
        }
    }

    /**
     * Get a pixel from the image
     * 
     * @param x the pixel x coordinate
     * @param y the pixel y coordinate
     * @return the corresponding pixel
     */
    public int getPixel(int x, int y) {
        return pixels[x][y] & 0xFFFFFF;
    }

    /**
     * Retrieve the buffered image
     * 
     * @return the buffered image
     */
    public BufferedImage getBufferedImage() {
        return bufferedImg;
    }

    /**
     * Retrieve the image width, in pixels
     * 
     * @return the image width
     */
    public int getWidth() {
        return bufferedImg != null ? bufferedImg.getWidth() : 0;
    }

    /**
     * Retrieve the image height, in pixels
     * 
     * @return the image height
     */
    public int getHeight() {
        return bufferedImg != null ? bufferedImg.getHeight() : 0;
    }

    /**
     * Save the image in a file
     * 
     * @param name the name under which to save the image
     */
    public void save(String name) {
        try {
            File file = new File(name + ".png");
            ImageIO.write(bufferedImg, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
