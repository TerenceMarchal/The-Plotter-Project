package image_processing.generators;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

import common.engine.Ink;
import common.engine.Utils;
import image_processing.engine.Brush;
import image_processing.engine.BrushPalette;
import image_processing.engine.ColorPalette;
import image_processing.enums.ColorAttributionMethod;
import image_processing.enums.ColorSpace;
import image_processing.enums.Setting;
import image_processing.session.Configuration;
import image_processing.session.Project;
import image_processing.transformations.ColorQuantizationTransformation;
import image_processing.transformations.TransformationStep;

/**
 * An abstract class used to generate BrushPalettes
 * 
 * @author Terence
 *
 */
public abstract class BrushPaletteGenerator {

    /**
     * Generate a BrushPalette directly from the available Inks
     * 
     * @param nbColors the number of colors of the BrushPalette to generate (note: should be a multiple of the number of
     *                 Inks available)
     * @return the generated BrushPalette
     */
    public static BrushPalette generateBrushPaletteFromInks(int nbColors) {
        int nbAvailableInks = Ink.getNbAvailableInks();
        if (nbColors % nbAvailableInks != 0) {
            nbColors -= nbColors % nbAvailableInks;
            System.out.println(String.format(
                    "The number of colors should be a multiple of the number of inks available (%d), using %d colors",
                    nbAvailableInks, nbColors));
        }
        int nbColorsPerInk = nbColors / nbAvailableInks;
        BrushPalette brushPalette = new BrushPalette();
        // TODO-011: we should probably be able to get rid of the black color
        brushPalette.addBrush(new Brush(0xFFFFFF, Ink.getBlackestAvailableInk(), 0, 45, true, true));
        for (Ink ink : Ink.getAvailableInks()) {
            for (int idColor = 0; idColor < nbColorsPerInk; idColor++) {
                // TODO-011: should we generate the colors according to the color space used instead of always simply
                // linear?
                double level = (double) idColor / nbColorsPerInk;
                int r = (int) Math.round(ink.getR() + (0xFF - ink.getR()) * level);
                int g = (int) Math.round(ink.getG() + (0xFF - ink.getG()) * level);
                int b = (int) Math.round(ink.getB() + (0xFF - ink.getB()) * level);
                brushPalette.addBrush(new Brush(r << 16 | g << 8 | b, ink, 1 - level, 45, true, true));
            }
        }
        return brushPalette;
    }

    /**
     * Generate a BrushPalette from the specified settings
     * 
     * @param inputColorPalette       the input ColorPalette to use, containing the colors used as the BrushPalette
     *                                input colors
     * @param lpmmMax                 the maximum number of lines per mm to use in the BrushPalette
     * @param nbLevelsPerColor        the number of filling levels per color to use in the BrushPalette
     * @param graySaturationThreshold the gray saturation threshold to use, between 0.0 and 1.0, to know when to use
     *                                black ink instead of the colored inks
     * @param colorSpace              the color space to use to generate the BrushPalette
     * @param colorAttributionMethod  the color attribution method to use to generate the BrushPalette
     * @return the generated BrushPalette
     */
    public static BrushPalette generateBrushPalette(ColorPalette inputColorPalette, double lpmmMax,
            int nbLevelsPerColor, double graySaturationThreshold, ColorSpace colorSpace,
            ColorAttributionMethod colorAttributionMethod) {
        if (colorAttributionMethod == ColorAttributionMethod.ASSIGN_CLOSEST_QUANTIZED_INK) {
            ColorQuantizationTransformation trans = (ColorQuantizationTransformation) Project.Instance
                    .getTransformation(TransformationStep.COLOR_QUANTIZATION);
            return generateBrushPaletteFromInks(
                    Configuration.Instance.getCurrentSettings().getIntSetting(Setting.NB_QUANTIZATION_COLORS));
        } else {
            /* index: color-ish, values: vector of colors that are color-ish */
            HashMap<Ink, Vector<Integer>> ishColors = new HashMap<Ink, Vector<Integer>>();
            for (Ink inkColor : Ink.getAvailableInks()) {
                ishColors.put(inkColor, new Vector<Integer>());
            }
            for (int color : inputColorPalette.getColors()) {
                Ink closestInkColor = Utils.getClosestInk(color, colorSpace, graySaturationThreshold);
                ishColors.get(closestInkColor).add(color);
            }

            BrushPalette brushPalette = new BrushPalette();
            for (Ink ink : ishColors.keySet()) {
                Vector<Integer> inputColors = ishColors.get(ink);
                for (Integer inputColor : inputColors) {
                    double level = 0;
                    int r = (inputColor >> 16) & 0xFF;
                    int g = (inputColor >> 8) & 0xFF;
                    int b = inputColor & 0xFF;
                    level = 255 * (1 - (double) Utils.distanceBetweenColors(inputColor, ink.getColorAsRgb(), colorSpace)
                            / Utils.distanceBetweenColors(0xFFFFFF, ink.getColorAsRgb(), colorSpace));

                    if (colorAttributionMethod == ColorAttributionMethod.ASSIGN_CLOSEST_COLOR) {
                        if (ink.isBlackestAvailableInk()) {
                            level = (double) (Math.round((level) / 255 * nbLevelsPerColor)) / nbLevelsPerColor;
                        } else {
                            level = (1 + Math.floor((255 - level) / 255 * nbLevelsPerColor)) / nbLevelsPerColor;
                        }
                    } else if (colorAttributionMethod == ColorAttributionMethod.FAVOR_MORE_CONTRASTS) {
                        if (ink.isBlackestAvailableInk()) {
                            level = (level) / 255;
                        } else {
                            level = (255 - level) / 255;
                        }

                    } else {
                        level = 1;
                        System.err.println("unsupported ColorAttributionMethod:" + colorAttributionMethod);
                    }
                    int angle = 45;
                    brushPalette.addBrush(new Brush(inputColor, ink, level, angle, true, true));
                }
            }

            if (colorAttributionMethod == ColorAttributionMethod.FAVOR_MORE_CONTRASTS) {
                BrushPalette distributedBrushPalette = new BrushPalette();
                for (Ink ink : Ink.getAvailableInks()) {
                    Vector<Brush> brushsOfInkColor = brushPalette.getBrushesOfInkColor(ink);
                    brushsOfInkColor.sort(new Comparator<Brush>() {
                        @Override
                        public int compare(Brush o1, Brush o2) {
                            return (int) ((o1.getLevel() - o2.getLevel()) * 1000);
                        }
                    });
                    int nbBrushsOfInkColor = brushsOfInkColor.size();
                    for (int idBrushOfInkColor = 0; idBrushOfInkColor < nbBrushsOfInkColor; idBrushOfInkColor++) {
                        Brush brush = brushsOfInkColor.get(idBrushOfInkColor);
                        double level = (double) (idBrushOfInkColor) / nbBrushsOfInkColor;
                        if (ink.isBlackestAvailableInk()) {
                            level = (double) (Math.round(level * nbLevelsPerColor)) / nbLevelsPerColor;
                        } else {
                            level = (1 + Math.floor(level * nbLevelsPerColor)) / nbLevelsPerColor;
                        }
                        Brush distributedBrush = new Brush(brush.getInputColor(), brush.getInk(), level,
                                brush.getAngle(), brush.isFineOutliningEnabled(), brush.isThickOutliningEnabled());
                        distributedBrushPalette.addBrush(distributedBrush);
                    }
                }
                brushPalette = distributedBrushPalette;
            }

            return brushPalette;
        }
    }

}
