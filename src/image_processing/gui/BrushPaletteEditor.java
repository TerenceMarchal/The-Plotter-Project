package image_processing.gui;

import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.JPanel;

import image_processing.engine.Brush;
import image_processing.engine.BrushPalette;
import image_processing.listeners.BrushEditorListener;

/**
 * A Brush Palette Editor, to edit a palette of Brushs
 * 
 * @author Terence
 *
 */
public class BrushPaletteEditor extends JPanel {

    /**
     * A BrushEditorListener to notify when a Brush configuration is modified by the user
     */
    private BrushEditorListener brushEditorListener;

    /**
     * Instantiate a Brush Palette Editor
     * 
     * @param brushEditorListener the BrushEditorListener to notify when a Brush configuration is modified by the user
     */
    public BrushPaletteEditor(BrushEditorListener brushEditorListener) {
        this.brushEditorListener = brushEditorListener;
        setLayout(new GridLayout(0, 1));
    }

    /**
     * Replace the currently linked BrushPalette by a new one
     * 
     * @param brushPalette the new BrushPalette to link to this editor
     */
    public void setBrushPalette(BrushPalette brushPalette) {
        if (brushPalette != null) {
            removeAll();
            for (Brush brush : brushPalette.getBrushesSortedByInputColor()) {
                add(new BrushEditor(brush, brushEditorListener));
            }
            revalidate();
        }
    }

    /**
     * Get the BrushPalette linked to this editor
     * 
     * @return the BrushPalette linked to this editor
     */
    public BrushPalette getBrushPalette() {
        BrushPalette brushPalette = new BrushPalette();
        for (Component comp : getComponents()) {
            if (comp instanceof BrushEditor) {
                BrushEditor brushEditor = (BrushEditor) comp;
                brushPalette.addBrush(brushEditor.getBrush());
            }
        }
        return brushPalette;
    }

}
