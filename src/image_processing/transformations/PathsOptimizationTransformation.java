package image_processing.transformations;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Vector;

import common.engine.Ink;
import common.engine.Line;
import common.engine.Path;
import common.engine.SettingsSet;
import common.engine.Utils;
import image_processing.engine.Image;
import image_processing.enums.OutputMirroring;
import image_processing.enums.OutputPosition;
import image_processing.enums.Setting;
import image_processing.session.Configuration;
import image_processing.session.Project;
import streaming.enums.PlotterSetting;
import streaming.session.PlotterConfiguration;

/**
 * A Transformation that optimize the drawing paths
 * 
 * @author Terence
 *
 */
public class PathsOptimizationTransformation extends AbstractTransformation {

    /**
     * Disable completely the path optimization
     * 
     * Note: mainly for debug, as the optimization is still slow and not always useful
     */
    private static final boolean DISABLE_PATH_OPTIMIZATION = true;

    /**
     * The original (i.e. non-optimized) paths per ink
     */
    private HashMap<Ink, Path> originalPathsPerInk;

    /**
     * The optimized paths per ink
     */
    private HashMap<Ink, Path> optimizedPathsPerInk = new HashMap<Ink, Path>();

    /**
     * Instantiate a Paths Optimization Transformation
     */
    public PathsOptimizationTransformation() {
        super(TransformationStep.PATHS_OPTIMIZATION, new Setting[] {}, true);
    }

    /**
     * Get the original (i.e. non-optimized) paths per ink
     * 
     * @return the original paths per ink
     */
    public HashMap<Ink, Path> getOriginalPathsPerInk() {
        return originalPathsPerInk;
    }

    /**
     * Get the optimized paths per ink
     * 
     * @return the optimized paths per ink
     */
    public HashMap<Ink, Path> getOptimizedPathsPerInk() {
        return optimizedPathsPerInk;
    }

    /**
     * Export the G-Code corresponding to the optimized paths
     */
    public void exportGCode() {
        /* create export directory */
        String projectName = settingsValues.getFileSetting(Setting.IMAGE_FILE).getName();
        projectName = projectName.substring(0, projectName.lastIndexOf("."));
        File exportDir = new File(projectName);
        if (exportDir.exists()) {
            exportDir.delete();
        }
        exportDir.mkdir();
        Configuration.Instance.save(projectName + "/" + projectName);

        /* generate and export G-Code */
        int imageDpi = settingsValues.getIntSetting(Setting.IMAGE_DPI);
        int rotationAngle = settingsValues.getOutputRotationSetting(Setting.OUTPUT_ROTATION).getAngle();
        Image originalImage = Project.Instance.getTransformation(TransformationStep.IMAGE_IMPORT).getOutputImage();
        double xMax = Utils.pxToMm(originalImage.getWidth(), imageDpi);
        double yMax = Utils.pxToMm(originalImage.getHeight(), imageDpi);
        OutputPosition position = settingsValues.getOutputPositionSetting(Setting.OUTPUT_POSITION);
        OutputMirroring mirroring = settingsValues.getOutputMirroringSetting(Setting.OUTPUT_MIRRORING);
        double trX = position.getXPosInA4();
        double trY = position.getYPosInA4();
        if (position.isCentered()) {
            trX -= xMax / 2;
            trY -= yMax / 2;
        }

        double clearZHeight = settingsValues.getDoubleSetting(Setting.CLEAR_Z_HEIGHT);
        int nbLinesToGenerate = 0;
        int nbLinesGenerated = 0;
        for (Path path : optimizedPathsPerInk.values()) {
            nbLinesToGenerate += path.getNbLines();
        }
        try {
            String filename = projectName + "/" + projectName + ".gcode";
            BufferedWriter allColorsBw = new BufferedWriter(new FileWriter(filename));
            double feedrate = Utils.speedToFeedrate(
                    PlotterConfiguration.Instance.getDoubleSettingValue(PlotterSetting.FLYING_XY_MAX_SPEED),
                    PlotterConfiguration.Instance.getBooleanSettingValue(PlotterSetting.IS_CORE_XY));
            String header = String.format(Locale.US, "G21 (use mm)\nG90 (use absolute coordinates)\nF%.0f\n", feedrate);
            allColorsBw.write(header);
            for (Ink ink : Ink.getAvailableInks()) {
                filename = projectName + "/" + projectName + "-" + ink.getName() + ".gcode";
                Path path = optimizedPathsPerInk.get(ink);
                if (path.getNbLines() > 0) {
                    allColorsBw.write(String.format(Locale.US, "T%d M6 ;change pen color: %s\n", ink.getColorAsRgb(),
                            ink.getName()));

                    BufferedWriter singleColorBw = new BufferedWriter(new FileWriter(filename));
                    singleColorBw.write(header);

                    double lastX = Double.MAX_VALUE;
                    double lastY = Double.MAX_VALUE;
                    ArrayList<Line> lines = path.getLines();
                    for (int idLine = 0; idLine < lines.size(); idLine++) {
                        double progression = (double) (nbLinesGenerated + idLine) / nbLinesToGenerate;
                        setProgression("Exporting G-Code...", progression, false);
                        Line line = lines.get(idLine);
                        Line transformedLine = Line.createTranslatedLine(
                                Line.createMirroredLine(Line.createRotatedLine(line, rotationAngle, xMax / 2, yMax / 2),
                                        mirroring, xMax, yMax),
                                trX, trY);
                        if (transformedLine.x0 != lastX || transformedLine.y0 != lastY) {
                            multipleWrite(singleColorBw, allColorsBw,
                                    String.format(Locale.US, "G0 Z%f\n", clearZHeight));
                            multipleWrite(singleColorBw, allColorsBw,
                                    String.format(Locale.US, "G0 X%f Y%f\n", transformedLine.x0, transformedLine.y0));
                            multipleWrite(singleColorBw, allColorsBw, "G0 Z0\n");
                        }
                        multipleWrite(singleColorBw, allColorsBw,
                                String.format(Locale.US, "G1 X%f Y%f\n", transformedLine.x1, transformedLine.y1));
                        lastX = transformedLine.x1;
                        lastY = transformedLine.y1;
                    }
                    multipleWrite(singleColorBw, allColorsBw, String.format(Locale.US, "G0 Z%f\n", clearZHeight));
                    singleColorBw.write(String.format(Locale.US, "G0 X0 Y0 Z%f\n", clearZHeight));
                    singleColorBw.close();

                } else {
                    new File(filename).delete();
                }
                nbLinesGenerated += path.getNbLines();
            }

            allColorsBw.write("T-1 M6 ;unload pen\n");
            allColorsBw.write(String.format(Locale.US, "G0 X0 Y0 Z%f\n", clearZHeight));
            allColorsBw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setProgression("Done", 1.0, false);
    }

    /**
     * Write data in two files at the same time
     * 
     * @param bw0  the BufferedWriter of the first file
     * @param bw1  the BufferedWriter of the second file
     * @param data the data to write
     * @throws IOException an IOException in case of error
     */
    private void multipleWrite(BufferedWriter bw0, BufferedWriter bw1, String data) throws IOException {
        bw0.write(data);
        bw1.write(data);
    }

    /**
     * Optimize a path
     * 
     * TODO-024: rework and clean this algorithm
     * 
     * @param ink               the ink of the path to optimize
     * @param idPathToOptimize  the id of the path to optimize (useful for computing to overall progression)
     * @param nbPathsToOptimize the number of paths to optimize (useful for computing to overall progression)
     */
    public void optimizePath(Ink ink, int idPathToOptimize, int nbPathsToOptimize) {
        if (DISABLE_PATH_OPTIMIZATION) {
            this.setProgression(1, true);
            return;
        }
        Path originalPath = null;
        try {
            originalPath = (Path) optimizedPathsPerInk.get(ink).clone();
        } catch (CloneNotSupportedException e1) {
            e1.printStackTrace();
        }
        int nbLinesInPath = originalPath.getNbLines();
        if (nbLinesInPath == 0) {
            return;
        }

//        Path continousPath = getContinousPath(originalPath, 0);
//        System.out.println(String.format("original path:\tfsl=%d flyingDistance=%d",
//                originalPath.getSquaredFlyingDistance(), originalPath.getFlyingDistance()));
        int NB_PATHS = 10;
        int NB_GENERATIONS = 10;
        double MUTATION_RATE = 0.20;

        System.out.println(originalPath.getNbLines() + " lines");

        // TODO-024: also try to group the lines together and split when there is a long distance with another group
//        Line lastLine = originalPath.getLine(0);
//        for (int idLine = 0; idLine < originalPath.getNbLines(); idLine++) {
//            Line line = originalPath.getLine(idLine);
//            double d = line.getCostFrom(lastLine);
//            System.out.println(d);
//        }

        try {
            /* initial population */
            ArrayList<Path> paths = new ArrayList<Path>();
            paths.add(originalPath);
            System.out.println("original path:" + originalPath.getCost());
            for (int idPath = 1; idPath < NB_PATHS; idPath++) {
//            for (int idPath = 0; idPath < NB_PATHS; idPath++) {
                // TODO-024: use continuous path generated from a random starting point instead of shuffled copy
                Path shuffledPath = Path.getShuffledCopy(originalPath);
//                Path shuffledPath = getContinousPath(originalPath, (int) (Math.random() * nbLinesInPath));
                shuffledPath = Path.optimizeLinesDirections(shuffledPath);
                paths.add(shuffledPath);
            }

            for (int idGen = 0; idGen < NB_GENERATIONS; idGen++) {
                System.out.println("generation " + idGen + " -----------------");
                this.setProgression((double) idPathToOptimize / nbPathsToOptimize
                        + (double) idGen / NB_GENERATIONS / nbPathsToOptimize, true);
                if (shouldAbortUpdate) {
                    return;
                }
                /* selection by roulette wheel */
                int idBestPath = 0;
                double bestFitness = 0;
                double sumFitness = 0;
                double[] sumFitnessAtPath = new double[NB_PATHS];
                for (int idPath = 0; idPath < NB_PATHS; idPath++) {
                    double fitness = paths.get(idPath).getFitness();
                    if (fitness > bestFitness) {
                        idBestPath = idPath;
                        bestFitness = fitness;
                    }
                    sumFitness += fitness;
                    sumFitnessAtPath[idPath] = sumFitness;
                }

                for (int idPath = 0; idPath < NB_PATHS; idPath++) {
                    sumFitnessAtPath[idPath] /= sumFitness;
                }

                ArrayList<Path> selectedPaths = new ArrayList<Path>();
                ArrayList<Path> nextGenerationPaths = new ArrayList<Path>();
                selectedPaths.add((Path) paths.get(idBestPath).clone());
                nextGenerationPaths.add((Path) paths.get(idBestPath).clone());
                while (selectedPaths.size() < NB_PATHS) {
                    // TODO-024: the wheel keep selecting the initial path for every children leading to almost no
                    // improvement, find a way to avoid that (only with outlining actually, because very well optimized)
                    double pos = Math.random();
                    int idPath = 0;
                    while (sumFitnessAtPath[idPath] < pos) {
                        idPath++;
                    }
                    selectedPaths.add((Path) paths.get(idPath).clone());
//                    // TODO-024: improve this basic random selection (because of the roulette issue, see above)
//                    int idPath = (int) (Math.random() * NB_PATHS);
//                    selectedPaths.add((Path) paths.get(idPath).clone());
                }

                /* crossover */
                int nbLines = originalPath.getNbLines();
                for (int idCouple = 0; idCouple < NB_PATHS / 2; idCouple++) {
                    Path path0 = selectedPaths.get(idCouple * 2);
                    Path path1 = selectedPaths.get(idCouple * 2 + 1);

                    Path p0 = (Path) path0.clone();
                    Path p1 = (Path) path1.clone();

//                    p0 = new Path(1);
//                    nbLines = 26;
//                    for (int i = 0; i < nbLines; i++) {
//                        p0.addLine(new Line(i, 0, 0, 0));
//                    }
//                    p1 = Path.getShuffledCopy(p0);

                    Path child0 = (Path) p0.clone();
                    Path child1 = (Path) p1.clone();

                    int crossoverFrom = (int) (Math.random() * nbLines);
                    int crossoverTo = (int) (Math.random() * nbLines);
                    if (crossoverFrom > crossoverTo) {
                        int tempCrossover = crossoverFrom;
                        crossoverFrom = crossoverTo;
                        crossoverTo = tempCrossover;
                    }
                    for (int idLine = crossoverFrom; idLine <= crossoverTo; idLine++) {
                        Line tempLine = (Line) child0.getLine(idLine).clone();
                        child0.setLine(idLine, (Line) child1.getLine(idLine).clone());
                        child1.setLine(idLine, tempLine);
                    }
                    Vector<Integer> idLinesToReplaceInChild0 = new Vector<Integer>();
                    Vector<Line> linesToAddToChild0 = new Vector<Line>();
                    Vector<Integer> idLinesToReplaceInChild1 = new Vector<Integer>();
                    Vector<Line> linesToAddToChild1 = new Vector<Line>();
                    for (int idLine = nbLines - 1; idLine >= 0; idLine--) {
                        Line line0 = child0.getLine(idLine);
                        int idLineInChild0 = child0.getIndexInPath(line0);
                        if (idLineInChild0 < idLine) {
                            idLinesToReplaceInChild0.add(idLine);
                            linesToAddToChild1.add(line0);
                        }
                        Line line1 = child1.getLine(idLine);
                        int idLineInChild1 = child1.getIndexInPath(line1);
                        if (idLineInChild1 < idLine) {
                            idLinesToReplaceInChild1.add(idLine);
                            linesToAddToChild0.add(line1);
                        }
                    }

                    for (int idLine = 0; idLine < idLinesToReplaceInChild0.size(); idLine++) {
                        child0.setLine(idLinesToReplaceInChild0.get(idLine), linesToAddToChild0.get(idLine));
                    }
                    for (int idLine = 0; idLine < idLinesToReplaceInChild1.size(); idLine++) {
                        child1.setLine(idLinesToReplaceInChild1.get(idLine), linesToAddToChild1.get(idLine));
                    }

                    for (int i = 0; i < nbLines; i++) { // TODO-024: remove this test when sure about it
                        if (child0.getIndexInPath(p0.getLine(i)) == -1) {
                            System.out.println(p0.getLine(i) + " missing in child0");
                        }
                        if (child1.getIndexInPath(p0.getLine(i)) == -1) {
                            System.out.println(p1.getLine(i) + " missing in child1");
                        }
                    }

                    // TODO-024: is it useful to optimize path direction every time? it is costly and slow the process
                    // ->
                    // not really, already optimized on most of the lines
//                    nextGenerationPaths.add(Path.optimizeLinesDirections(child0));
                    nextGenerationPaths.add(child0);
//                    System.out.println(child0.getCost());
                    if (nextGenerationPaths.size() < NB_PATHS) {
//                        nextGenerationPaths.add(Path.optimizeLinesDirections(child1));
                        nextGenerationPaths.add(child1);
//                        System.out.println(child1.getCost());
                    }

                }
                paths = (ArrayList<Path>) nextGenerationPaths.clone();
                Path bestPath = paths.get(0);
                double averageCost = 0;
                for (Path path : paths) {
                    double cost = path.getCost();
                    if (cost < bestPath.getCost()) {
                        bestPath = path;
                    }
                    averageCost += cost;
                }
                System.out.println("best path:" + bestPath.getCost());
                optimizedPathsPerInk.put(ink, (Path) bestPath.clone()); // TODO-024: is clone needed?
                averageCost /= NB_PATHS;
//                double flyingOptimRatio = (double) (bestPath.getFlyingDistance() - originalPath.getFlyingDistance())
//                        / originalPath.getFlyingDistance();
//                double totalOptimRatio = (double) (bestPath.getTotalDistance() - originalPath.getTotalDistance())
//                        / originalPath.getTotalDistance();
//                System.out.println("best: " + bestPath);
//                if (idGen == NB_GENERATIONS - 1) {
//                    System.out.println(String.format(
//                            "%dth generation:\tflyingDistance=%d totalDistance=%d flyingRatio=%.1f%% flyingOptimRatio=%.1f%% totalOptimRatio=%.1f%%",
//                            idGen + 1, bestPath.getFlyingDistance(), bestPath.getTotalDistance(),
//                            bestPath.getFlyingRatio() * 100, flyingOptimRatio * 100, totalOptimRatio * 100));
//                }

                /* mutations */
                if (idGen < NB_GENERATIONS - 1) {
                    /* Note: start at 1 to avoid mutating the best one */
                    for (int idPath = 1; idPath < NB_PATHS; idPath++) {
                        if (Math.random() < MUTATION_RATE) {
                            Path path = paths.get(idPath);
                            Path mutatedPath = new Path(originalPath.getZClearHeight());
                            int mutationType = (int) (Math.random() * 2);
//                            System.out.println("mutation " + mutationType);
                            if (mutationType == 0) {
                                /* path direction inversion */
                                int startMutation = (int) (Math.random() * nbLines);
                                int endMutation = (int) (Math.random() * nbLines);
                                if (startMutation > endMutation) {
                                    int temp = startMutation;
                                    startMutation = endMutation;
                                    endMutation = temp;
                                }
                                for (int idLine = 0; idLine < nbLinesInPath; idLine++) {
                                    if (idLine >= startMutation && idLine <= endMutation) {
                                        int t = endMutation - (idLine - startMutation);
                                        mutatedPath.addLine(Line.createInvertedLine(path.getLine(t)));
                                    } else {
                                        mutatedPath.addLine(path.getLine(idLine));
                                    }
                                }
                                // TODO-024: should we optimize line direction every time?
//                                if (mutatedPath.getCost() < path.getCost()) {
//                                    System.out.println("mutated best: " + mutatedPath.getCost() + " " + path.getCost());
//                                } else {
//                                    if (Path.optimizeLinesDirections(mutatedPath).getCost() < Path
//                                            .optimizeLinesDirections(path).getCost()) {
//                                        System.out.println("optimized mutated best: " + mutatedPath.getCost() + " "
//                                                + path.getCost());
//                                    }
//                                }
                            } else if (mutationType == 1) {
                                /* path shifting */
                                int shift = (int) (Math.random() * nbLines);
                                for (int idLine = 0; idLine < nbLinesInPath; idLine++) {
                                    mutatedPath.addLine(path.getLine((shift + idLine) % nbLines));
                                }
                            }
//                             else if (mutationType == 2) {
//                                /* area shifting */
//                                Line previousLine = path.getLine(0);
//                                for (int idLine = 1; idLine < nbLinesInPath; idLine++) {
//                                    Line line = path.getLine(idLine);
//                                    System.out.println(line.getCostFrom(previousLine));
//                                    previousLine = line;
//                                }
//                            }
                        }
                    }
                }
            }
            // TODO-024: check that the optimized path contain the same lines as the original to check that there was
            // no
            // mistake
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
//        System.out.println("optimized path:" + optimizedPathsPerInk.get(inkColor).getCost());

//        return optimizedPath;
    }

    @Override
    public void drawVectorizedImageOutput(Graphics2D g) {
        g.setColor(Color.black);
        Stroke defaultStroke = g.getStroke();
        g.setStroke(Utils.getPenStrokeInMm(settingsValues.getDoubleSetting(Setting.PEN_TIP_DIAMETER)));

        for (Entry<Ink, Path> entry : optimizedPathsPerInk.entrySet()) {
            g.setColor(Utils.getLighterColor(entry.getKey().getColor()));
            Path path = entry.getValue();
            Line lastLine = null;
            for (Line line : path.getLines()) {
                if (lastLine != null && (line.x0 != lastLine.x1 || line.y0 != lastLine.y1)) {
                    g.draw(new Line2D.Double(
                            Utils.mmToPxDouble(lastLine.x1, settingsValues.getIntSetting(Setting.IMAGE_DPI)),
                            Utils.mmToPxDouble(lastLine.y1, settingsValues.getIntSetting(Setting.IMAGE_DPI)),
                            Utils.mmToPxDouble(line.x0, settingsValues.getIntSetting(Setting.IMAGE_DPI)),
                            Utils.mmToPxDouble(line.y0, settingsValues.getIntSetting(Setting.IMAGE_DPI))));
                }
                lastLine = line;
            }
        }

        g.setStroke(defaultStroke);
    }

    @Override
    protected Image executeTransformation(AbstractTransformation previousTransformation, SettingsSet settings) {
        originalPathsPerInk = ((PathsGenerationTransformation) previousTransformation).getPathsPerInk();
        optimizedPathsPerInk = (HashMap<Ink, Path>) ((PathsGenerationTransformation) previousTransformation)
                .getPathsPerInk().clone();
        int idInk = 0;
        for (Entry<Ink, Path> entry : optimizedPathsPerInk.entrySet()) {
            Ink ink = entry.getKey();
            optimizePath(ink, idInk, optimizedPathsPerInk.size());
            idInk++;
        }
        return null;
    }

}
