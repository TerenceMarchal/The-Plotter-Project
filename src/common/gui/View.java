package common.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import common.engine.Zoom;

/**
 * A View, that can be dragged, zoomed-on, etc.
 * 
 * @author Terence
 *
 */
public abstract class View extends JPanel implements MouseWheelListener, MouseMotionListener {

    /**
     * The JPanel actually containing the view content
     */
    protected final JPanel viewPanel;

    /**
     * The View Zoom
     */
    protected final Zoom zoom;

    /**
     * The previous mouse coordinates
     */
    protected double previousMouseX, previousMouseY;

    /**
     * The current drag origin coordinates
     */
    private double dragOriginX, dragOriginY;

    /**
     * Instantiate a new View with the specified Zoom
     * 
     * @param viewName the name of the View
     * @param zoom     the Zoom to use for the View
     */
    public View(String viewName, Zoom zoom) {
        setBorder(BorderFactory.createTitledBorder(viewName));
        setLayout(new BorderLayout());
        this.zoom = zoom;
        viewPanel = new JPanel() {
            @Override
            public void paintComponent(Graphics G) {
                Graphics2D g = (Graphics2D) G;
                int W = getWidth();
                int H = getHeight();
                g.setColor(Color.white);
                g.fillRect(0, 0, W, H);
                g.setColor(Color.black);
                double zoomLevel = zoom.getZoomLevel();
                g.scale(zoomLevel, zoomLevel);
                g.translate(-zoom.getTranslationX(), -zoom.getTranslationY());
                View.this.paint(g, W, H);
                g.translate(zoom.getTranslationX(), zoom.getTranslationY());
                g.scale(1 / zoomLevel, 1 / zoomLevel);

            }
        };
        viewPanel.addMouseWheelListener(this);
        viewPanel.addMouseMotionListener(this);
        add(viewPanel, BorderLayout.CENTER);
    }

    /**
     * Instantiate a new View
     * 
     * @param viewName the name of the View
     */
    public View(String viewName) {
        this(viewName, new Zoom());
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        double previousZoom = zoom.getZoomLevel();
        double currentZoom = zoom.getZoomLevel();
        if (e.getUnitsToScroll() < 0) {
            currentZoom *= 1.1;
        } else {
            currentZoom *= 0.9;
        }

        double trX = zoom.getTranslationX();
        double trY = zoom.getTranslationY();
        double trMouseX = trX + e.getX() / currentZoom;
        double trMouseY = trY + e.getY() / currentZoom;
        trX = (int) (trMouseX + previousZoom / currentZoom * (trX - trMouseX));
        trY = (int) (trMouseY + previousZoom / currentZoom * (trY - trMouseY));
        zoom.setZoolLevelAndTranslation(currentZoom, trX, trY);

        this.getParent().repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (SwingUtilities.isMiddleMouseButton(e)) {
            double trX = zoom.getTranslationX();
            double trY = zoom.getTranslationY();
            trX += (previousMouseX - e.getX()) / zoom.getZoomLevel();
            trY += (previousMouseY - e.getY()) / zoom.getZoomLevel();
            zoom.setTranslation(trX, trY);

            previousMouseX = e.getX();
            previousMouseY = e.getY();
            this.getParent().repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        previousMouseX = e.getX();
        previousMouseY = e.getY();
    }

    /**
     * Get the mouse X coordinate according to the current zoom and view translation
     * 
     * @param e the MouseEvent from which to get the mouse X coordinate
     * @return the mouse X coordinate according to the current zoom and view translation
     */
    protected double getZoomedMouseX(MouseEvent e) {
        return e.getX() / zoom.getZoomLevel() + zoom.getTranslationX();
    }

    /**
     * Get the mouse Y coordinate according to the current zoom and view translation
     * 
     * @param e the MouseEvent from which to get the mouse Y coordinate
     * @return the mouse Y coordinate according to the current zoom and view translation
     */
    protected double getZoomedMouseY(MouseEvent e) {
        return e.getY() / zoom.getZoomLevel() + zoom.getTranslationY();
    }

    /**
     * Paint the View content
     * 
     * @param g the Graphics2D with which to paint the View content
     * @param W the View width
     * @param H the View height
     */
    protected abstract void paint(Graphics2D g, int W, int H);
}
