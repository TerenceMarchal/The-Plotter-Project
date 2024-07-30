package common.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import image_processing.gui.StatusBar;
import image_processing.gui.Toolbox;
import image_processing.gui.TransformationResultView;
import image_processing.session.Project;
import image_processing.transformations.TransformationStep;
import streaming.gui.ControlBar;
import streaming.gui.ManualControlPanel;
import streaming.gui.PlotterView;
import streaming.gui.StreamingStatusBar;

/**
 * The GUI Window
 * 
 * @author Terence
 *
 */
public class Window extends JFrame {

    /**
     * Indicate if the Plotter View should be opened at startup
     * 
     * TODO-028: save the last opened view and re-open it at startup instead
     */
    private static final boolean OPEN_PLOTTER_VIEW_AT_STARTUP = false;

    /**
     * The singleton instance of the GUI Window
     */
    public static Window Instance;

    /**
     * The GUI Plotter View
     */
    private PlotterView plotterView;

    /**
     * Instantiate a Window
     */
    private Window() {
        Instance = this;
        /* Look & Feel */
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Image To G-Code Tool", new ImageIcon("data/icons/drawing.png"), initEditorPanel());
        tabs.addTab("Plotter Controler", new ImageIcon("data/icons/plotter.png"), initStreamingPanel());
        tabs.setSelectedIndex(OPEN_PLOTTER_VIEW_AT_STARTUP ? 1 : 0);

        /* Window */
        setTitle("The Plotter Project");
        setSize(1280, 1024);
        setLocationRelativeTo(null);
        setContentPane(tabs);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    /**
     * Init the Editor Panel, containing all the Transformation Result Views and the Toolbox
     * 
     * @return the initialized Editor Panel
     */
    private JPanel initEditorPanel() {
        JPanel editorPanel = new JPanel(new BorderLayout());

        /* Views */
        JPanel views = new JPanel(new GridLayout(2, 3));
        views.add(new TransformationResultView("Source Image",
                Project.Instance.getTransformation(TransformationStep.IMAGE_IMPORT)));
        views.add(new TransformationResultView("Color-quantized Image",
                Project.Instance.getTransformation(TransformationStep.COLOR_QUANTIZATION)));
        views.add(new TransformationResultView("Recolored Image",
                Project.Instance.getTransformation(TransformationStep.RECOLORIZATION)));
        views.add(new TransformationResultView("Outlined Image",
                Project.Instance.getTransformation(TransformationStep.FINE_OUTLINING)));
        views.add(new TransformationResultView("Final Image",
                Project.Instance.getTransformation(TransformationStep.PATHS_GENERATION)));
        views.add(new TransformationResultView("Flying Motions",
                Project.Instance.getTransformation(TransformationStep.PATHS_OPTIMIZATION)));
        editorPanel.add(views, BorderLayout.CENTER);

        /* Toolbox */
        editorPanel.add(new Toolbox(), BorderLayout.EAST);

        /* Status bar */
        editorPanel.add(new StatusBar(), BorderLayout.SOUTH);

        return editorPanel;
    }

    /**
     * Init the Streaming Panel, containing the Plotter View, Manual Control Panel, etc.
     * 
     * @return the initialized Streaming Panel
     */
    private JPanel initStreamingPanel() {
        JPanel streamingPanel = new JPanel(new BorderLayout());

        streamingPanel.add(new ControlBar(), BorderLayout.NORTH);
        streamingPanel.add(new ManualControlPanel(), BorderLayout.WEST);
        plotterView = new PlotterView();
        streamingPanel.add(plotterView, BorderLayout.CENTER);
        streamingPanel.add(new StreamingStatusBar(), BorderLayout.SOUTH);

        return streamingPanel;
    }

    /**
     * Get the Plotter View
     * 
     * TODO-038: this should probably not be accessed through the Window class
     * 
     * @return the Plotter View
     */
    public PlotterView getPlotterView() {
        return plotterView;
    }

    /**
     * The application entry point
     * 
     * @param args the arguments passed to the application
     */
    public static void main(String[] args) {
        Project.initProject();
        new Window();
    }
}
