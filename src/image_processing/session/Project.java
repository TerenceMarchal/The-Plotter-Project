package image_processing.session;

import common.engine.SettingsSet;
import image_processing.listeners.ConfigurationChangeListener;
import image_processing.transformations.AbstractTransformation;
import image_processing.transformations.ColorQuantizationTransformation;
import image_processing.transformations.FineOutliningTransformation;
import image_processing.transformations.ImageImportTransformation;
import image_processing.transformations.PathsGenerationTransformation;
import image_processing.transformations.PathsOptimizationTransformation;
import image_processing.transformations.RecolorizationTransformation;
import image_processing.transformations.ThickOutliningTransformation;
import image_processing.transformations.TransformationStep;
import streaming.session.PlotterConfiguration;

/**
 * A class representing a image-processing project
 * 
 * @author Terence
 *
 */
public class Project implements ConfigurationChangeListener {

    /**
     * The Project singleton
     */
    public static Project Instance;

    /**
     * The Transformations to apply to an image to get the final result
     */
    private AbstractTransformation[] transformations;

    /**
     * The computation thread that will perform all the transformation computations
     */
    private ComputationThread computationThread;

    /**
     * Instantiate a new Project
     */
    private Project() {
        Configuration.Instance.addListener(this);
        transformations = new AbstractTransformation[] { new ImageImportTransformation(),
                new ColorQuantizationTransformation(), new RecolorizationTransformation(),
                new ThickOutliningTransformation(), new FineOutliningTransformation(),
                new PathsGenerationTransformation(), new PathsOptimizationTransformation() };
        // TODO-022: handle transformations "paths" that does not include all transformations
//        if (transformations.length != TransformationStep.values().length) {
//            System.err.println("Not all transformations steps have a transformation registered");
//            System.exit(-1);
//        }

        computationThread = new ComputationThread(transformations);

    }

    @Override
    public void configurationSettingsValuesChanged(SettingsSet settings) {
        computationThread.compute(settings);
    }

    /**
     * Get the Transformation corresponding to the specified step
     * 
     * @param transformationStep the Transformation step for which to retrieve the Transformation
     * @return the Transformation corresponding to the specified step
     */
    public AbstractTransformation getTransformation(TransformationStep transformationStep) {
        if (transformationStep.ordinal() > transformations.length) {
            System.err.println("No transformation for step " + transformationStep.name());
            return null;
        }
        return transformations[transformationStep.ordinal()];
    }

    /**
     * Get the Project computation thread
     * 
     * TODO-038: try to get rid of this function
     * 
     * @return the Project computation thread
     */
    public ComputationThread getComputationThread() {
        return computationThread;
    }

    /**
     * Init the Project
     */
    public static void initProject() {
        if (Instance == null) {
            PlotterConfiguration.initSingleton();
            Configuration.initSingleton();
            Instance = new Project();

        }
    }

}
