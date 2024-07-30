package streaming.listeners;

import java.util.HashMap;

import common.engine.Ink;
import streaming.engine.Job;

/**
 * A Listener to be notified about instruction streaming to the Plotter events and progression
 * 
 * @author Terence
 *
 */
public interface StreamingProgressionListener {

    /**
     * Callback called when a new Job has just started being streamed to the Plotter
     * 
     * @param job the Job that just stared
     */
    public void startedNewJob(Job job);

    /**
     * Callback called when the Job streaming progression has changed
     * 
     * @param elapsedDurationSinceJobStart      the elapsed duration in ms since the Job has started
     * @param nbInstructionsExecutedPerInk      an HashMap containing the number of instructions already executed per
     *                                          Ink
     * @param drawedDistancesPerInk             an HashMap containing the distances already draw per Ink
     * @param estimatedRemainingDurationsPerInk an HashMap containing the estimated remaining duration per Ink
     */
    public void streamingProgressionChanged(double elapsedDurationSinceJobStart,
            HashMap<Ink, Integer> nbInstructionsExecutedPerInk, HashMap<Ink, Double> drawedDistancesPerInk,
            HashMap<Ink, Integer> estimatedRemainingDurationsPerInk);

    /**
     * Callback called when a Job is completed
     */
    public void jobCompleted();
}
