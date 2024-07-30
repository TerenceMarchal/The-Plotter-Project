package image_processing.gui;

import java.awt.Font;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import common.engine.Ink;
import common.engine.Path;
import common.engine.Utils;
import common.enums.LabelType;
import image_processing.engine.Image;
import image_processing.enums.Setting;
import image_processing.listeners.ComputationProgressionListener;
import image_processing.session.Project;
import image_processing.transformations.AbstractTransformation;
import image_processing.transformations.PathsGenerationTransformation;
import image_processing.transformations.PathsOptimizationTransformation;
import image_processing.transformations.TransformationStep;

/**
 * The Status Bar, that contains informations about the paths distances, estimated durations, etc.
 * 
 * @author Terence
 *
 */
public class StatusBar extends JPanel implements ComputationProgressionListener {

    /**
     * The Status Bar labels
     */
    private JLabel imageDimensionsLabel, currentStepLabel, drawingDistanceLabel, flyingDistanceLabel,
            upDownDistanceLabel, estimatedDurationLabel, timeSavedLabel;

    /**
     * The Status Bar progress bar
     */
    private JProgressBar currentStepProgression;

    /**
     * Instantiate a new Status Bar
     */
    public StatusBar() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        Font boldFont = new JLabel().getFont().deriveFont(Font.BOLD);

        add(new JLabel("Image dimensions:"));
        imageDimensionsLabel = new JLabel();
        imageDimensionsLabel.setFont(boldFont);
        add(imageDimensionsLabel);

        add(new JLabel(" Current step:"));
        currentStepLabel = new JLabel();
        currentStepLabel.setFont(boldFont);
        add(currentStepLabel);

        currentStepProgression = new JProgressBar();
        add(currentStepProgression);

        add(new JLabel(" Drawing distance:"));
        drawingDistanceLabel = new JLabel();
        drawingDistanceLabel.setFont(boldFont);
        add(drawingDistanceLabel);
        add(new JLabel(" Flying distance:"));
        flyingDistanceLabel = new JLabel();
        flyingDistanceLabel.setFont(boldFont);
        add(flyingDistanceLabel);
        add(new JLabel(" Up/down distance:"));
        upDownDistanceLabel = new JLabel();
        upDownDistanceLabel.setFont(boldFont);
        add(upDownDistanceLabel);
        add(new JLabel(" Estimated duration:"));
        estimatedDurationLabel = new JLabel();
        estimatedDurationLabel.setFont(boldFont);
        add(estimatedDurationLabel);
        // TODO-024: add this label when the path optimization has been improved
//        add(new JLabel(" Time saved:"));
        timeSavedLabel = new JLabel();
        timeSavedLabel.setFont(boldFont);
//        add(timeSavedLabel);

        Project.Instance.getComputationThread().addComputationProgressionListener(this);
    }

    @Override
    public void currentTransformationChanged(AbstractTransformation currentTransformation) {
        /* update image dimensions label */
        Image image = Project.Instance.getTransformation(TransformationStep.IMAGE_IMPORT).getOutputImage();
        if (currentTransformation != null && image != null) {
            int imageDpi = currentTransformation.getSettingsValues().getIntSetting(Setting.IMAGE_DPI);
            double imgW = Utils.pxToMm(image.getWidth(), imageDpi);
            double imgH = Utils.pxToMm(image.getHeight(), imageDpi);
            imageDimensionsLabel.setText(String.format(Locale.US, "%.0fmm x %.0fmm", imgW, imgH));
        }

        /* update current step label */
        String label;
        if (currentTransformation != null) {
            TransformationStep trStep = currentTransformation.getTransformationStep();
            label = String.format(Locale.US, "%s (%d/%d)", trStep.getName(), trStep.getStep(), trStep.getNbSteps());
            currentStepProgression.setIndeterminate(true);
        } else {
            label = "Done";
            currentStepProgression.setIndeterminate(false);
            currentStepProgression.setValue(100);
        }
        currentStepLabel.setText(label);

        /* update gcode informations */
        if (currentTransformation instanceof PathsOptimizationTransformation) {
            PathsGenerationTransformation tr = (PathsGenerationTransformation) Project.Instance
                    .getTransformation(TransformationStep.PATHS_GENERATION);
            HashMap<Ink, Path> pathsPerInk = tr.getPathsPerInk();
            HashMap<Ink, Double> drawingDistances = new HashMap<Ink, Double>();
            double totalDrawingDistance = 0;
            for (Entry<Ink, Path> entry : pathsPerInk.entrySet()) {
                Ink ink = entry.getKey();
                Path path = entry.getValue();
                double drawingDist = path.getDrawingDistance();
                drawingDistances.put(ink, drawingDist);
                totalDrawingDistance += drawingDist;
            }

            drawingDistanceLabel.setText(Utils.beautifyDistance(totalDrawingDistance));
            drawingDistanceLabel
                    .setToolTipText(Utils.generateDoubleValuePerInkTooltip(drawingDistances, LabelType.DISTANCE));
        }
    }

    @Override
    public void currentTransformationProgressionChanged(AbstractTransformation currentTransformation,
            String progressionLabel, double currentTransformationProgression) {
        // TODO-054: fix the "Cannot read field "width" because "this.componentInnards" is null" error occurring here
        if (currentStepProgression.isIndeterminate()) {
            currentStepProgression.setIndeterminate(false);
        }
        currentStepProgression.setValue((int) Math.round(currentTransformationProgression * 100));

        /* update gcode informations */
        if (currentTransformation instanceof PathsOptimizationTransformation) {
            PathsOptimizationTransformation tr = (PathsOptimizationTransformation) currentTransformation;
            HashMap<Ink, Path> optimizedPathsPerInk = tr.getOptimizedPathsPerInk();
            HashMap<Ink, Path> originalPathsPerInk = tr.getOriginalPathsPerInk();
            HashMap<Ink, Double> flyingDistances = new HashMap<Ink, Double>();
            double totalFlyingDistance = 0;
            HashMap<Ink, Double> upDownDistances = new HashMap<Ink, Double>();
            double totalUpDownDistance = 0;
            HashMap<Ink, Double> estimatedDurations = new HashMap<Ink, Double>();
            double totalEstimatedDurations = 0;
            HashMap<Ink, Double> timesSaved = new HashMap<Ink, Double>();
            double totalTimeSaved = 0;
            for (Entry<Ink, Path> entry : optimizedPathsPerInk.entrySet()) {
                Ink ink = entry.getKey();
                Path path = entry.getValue();
                double flyingDist = path.getFlyingDistance();
                flyingDistances.put(ink, flyingDist);
                totalFlyingDistance += flyingDist;
                double upDownDist = path.getUpDownDistance();
                upDownDistances.put(ink, upDownDist);
                totalUpDownDistance += upDownDist;
                double duration = path.getTotalDuration();
                estimatedDurations.put(ink, duration);
                totalEstimatedDurations += duration;
                double timeSaved = originalPathsPerInk.get(ink).getTotalDuration() - duration;
                timesSaved.put(ink, timeSaved);
                totalTimeSaved += timeSaved;
            }

            if (progressionLabel != null) {
                currentStepLabel.setText(progressionLabel);
            }

            flyingDistanceLabel.setText(Utils.beautifyDistance(totalFlyingDistance));
            flyingDistanceLabel
                    .setToolTipText(Utils.generateDoubleValuePerInkTooltip(flyingDistances, LabelType.DISTANCE));

            upDownDistanceLabel.setText(Utils.beautifyDistance(totalUpDownDistance));
            upDownDistanceLabel
                    .setToolTipText(Utils.generateDoubleValuePerInkTooltip(upDownDistances, LabelType.DISTANCE));

            estimatedDurationLabel.setText(Utils.beautifyDuration(totalEstimatedDurations));
            estimatedDurationLabel
                    .setToolTipText(Utils.generateDoubleValuePerInkTooltip(estimatedDurations, LabelType.DURATION));

            timeSavedLabel.setText(Utils.beautifyDuration(totalTimeSaved));
            timeSavedLabel.setToolTipText(Utils.generateDoubleValuePerInkTooltip(timesSaved, LabelType.DURATION));
        }
    }

}
