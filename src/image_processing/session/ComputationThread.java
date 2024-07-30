package image_processing.session;

import java.util.Vector;

import common.engine.SettingsSet;
import image_processing.listeners.ComputationProgressionListener;
import image_processing.listeners.TransformationResultChangeListener;
import image_processing.transformations.AbstractTransformation;
import image_processing.transformations.PathsOptimizationTransformation;
import image_processing.transformations.TransformationStep;

/**
 * A thread used for all Transformation computation
 * 
 * @author Terence
 *
 */
public class ComputationThread extends Thread implements TransformationResultChangeListener {

    /**
     * The Transformations to compute
     */
    private final AbstractTransformation[] transformations;

    /**
     * The Transformation currently being computed
     */
    private AbstractTransformation transformationBeingComputed;

    /**
     * A flag indicating if we should force the recomputing of all the Transformations
     */
    private boolean forceRecomputingAllTransformations;

    /**
     * A flag indicating if we should export the final output G-Code
     */
    private boolean shouldExportGcode = false;

    /**
     * The settings used for the computations
     */
    private SettingsSet settings;

    /**
     * The settings used for the next re-computation
     */
    private SettingsSet settingsForNextRecomputation;

    /**
     * The computation progression listeners
     */
    private Vector<ComputationProgressionListener> listeners = new Vector<ComputationProgressionListener>();

    /**
     * Instantiate a new computation thread
     * 
     * @param transformations the transformations to compute
     */
    public ComputationThread(AbstractTransformation[] transformations) {
        this.transformations = transformations;
        for (AbstractTransformation transformation : transformations) {
            transformation.addListener(this);
        }
        forceRecomputingAllTransformations = true;
        start();
    }

    /**
     * Compute the Transformation with the specified settings
     * 
     * @param settings the SettingsSet to use for the computation
     */
    public void compute(SettingsSet settings) {
        settingsForNextRecomputation = settings;
        if (transformationBeingComputed != null) {
            transformationBeingComputed.abortUpdate();
        }
    }

    /**
     * Export the G-Code when the computations are done
     */
    public void exportGcode() {
        shouldExportGcode = true;
    }

    /**
     * Add a computation progression listener
     * 
     * @param listener the computation progression listener to add
     */
    public void addComputationProgressionListener(ComputationProgressionListener listener) {
        listeners.add(listener);
    }

    /**
     * Notify the computation progression listeners that the current Transformations step has changed
     * 
     * @param currentTransformation the new current Transformation
     */
    private void fireCurrentTransformationStepChanged(AbstractTransformation currentTransformation) {
        for (ComputationProgressionListener listener : listeners) {
            listener.currentTransformationChanged(currentTransformation);
        }
    }

    @Override
    public void run() {
        while (true) {
            if (forceRecomputingAllTransformations || settingsForNextRecomputation != null) {
                if (settingsForNextRecomputation != null) {
                    settings = (SettingsSet) settingsForNextRecomputation.clone();
                    settingsForNextRecomputation = null;
                }
                AbstractTransformation previousTransformation = null;
                boolean transformationUpdated = false;
                for (AbstractTransformation transformation : transformations) {
                    fireCurrentTransformationStepChanged(transformation);
                    if (forceRecomputingAllTransformations || transformationUpdated
                            || transformation.dependsOnSettings(settings.getSettingsNames())) {
                        transformationBeingComputed = transformation;
                        transformation.updateTransformation(previousTransformation,
                                Configuration.Instance.getCurrentSettings());
                        transformationUpdated = true;
                    }
                    previousTransformation = transformation;
                    if (!forceRecomputingAllTransformations && settingsForNextRecomputation != null) {
                        /* New settings have been applied, abort the current Transformation flow */
                        break;
                    }
                }
                fireCurrentTransformationStepChanged(null);
                transformationBeingComputed = null;
            }
            if (shouldExportGcode) {
                shouldExportGcode = false;
                ((PathsOptimizationTransformation) Project.Instance
                        .getTransformation(TransformationStep.PATHS_OPTIMIZATION)).exportGCode();
            }
            forceRecomputingAllTransformations = false;
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void transformationResultChanged(AbstractTransformation transformation) {
        /* nothing to do */
    }

    @Override
    public void transformationProgressionChanged(AbstractTransformation transformation, String progressionLabel,
            double progression, boolean shouldRepaint) {
        for (ComputationProgressionListener listener : listeners) {
            listener.currentTransformationProgressionChanged(transformation, progressionLabel, progression);
        }
    }

}
