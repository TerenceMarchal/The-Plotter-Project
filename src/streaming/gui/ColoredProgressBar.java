package streaming.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.util.HashMap;
import java.util.Set;

import javax.swing.JPanel;

import common.engine.Ink;
import common.engine.Utils;

/**
 * A progress bar GUI element with multiple colors, to show multiple progressions at once
 * 
 * @author Terence
 *
 */
public class ColoredProgressBar extends JPanel {

    /**
     * The Inks colors to show, ordered from left to right on the progress bar
     */
    private Set<Ink> orderedColors;

    /**
     * The current progression values per Ink color
     */
    private HashMap<Ink, Double> progressionValuesPerColor = new HashMap<Ink, Double>();

    /**
     * The maximum progression values per Ink color
     */
    private HashMap<Ink, Double> progressionMaxPerColor = new HashMap<Ink, Double>();

    /**
     * The sum of the maximum progression values for all the Ink colors
     */
    private double progressionMaxForAllColors = 0;

    /**
     * Set the Ink colors key order
     * 
     * @param orderedColors the Ink colors for which to show the progression, ordered from left to right
     */
    public void setColorOrder(Set<Ink> orderedColors) {
        this.orderedColors = orderedColors;
    }

    /**
     * Set the current progression
     * 
     * Note: setColorOrder() must have been called previously to specify the colors to display and set their order
     * 
     * @param progressionValuesPerColor the current progression values per color
     * @param progressionMaxPerColor    the maximum progression values per color
     */
    public void setProgress(HashMap<Ink, Double> progressionValuesPerColor,
            HashMap<Ink, Double> progressionMaxPerColor) {
        this.progressionValuesPerColor = progressionValuesPerColor;
        this.progressionMaxPerColor = progressionMaxPerColor;
        progressionMaxForAllColors = 0;
        for (Double max : progressionMaxPerColor.values()) {
            progressionMaxForAllColors += max;
        }
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        int W = getWidth();
        int H = getHeight();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);

        if (orderedColors != null) {
            double x = 0;
            for (Ink ink : orderedColors) {
                g.setColor(Utils.getLighterColor(ink.getColor()));
                double wMax = progressionMaxPerColor.get(ink) / progressionMaxForAllColors * W;
                g.fillRect((int) Math.floor(x), 0, (int) Math.ceil(wMax), H);

                g.setColor(ink.getColor());
                double w = progressionValuesPerColor.get(ink) / progressionMaxForAllColors * W;
                g.fillRect((int) Math.floor(x), 0, (int) Math.ceil(w), H);
                x += wMax;
            }
        }

        g.setColor(Color.BLACK);
        g.drawRect(0, 0, W, H);
    }
}
