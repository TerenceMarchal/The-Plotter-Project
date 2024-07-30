package streaming.enums;

/**
 * The available Job rendering types
 * 
 * @author Terence
 *
 */
public enum JobRenderingType {

    JOB_STREAMING, /* a Job currently being streamed to the Plotter */
    JOB_COMPLETED, /* a Job which has been completed */
    JOB_PREVIEW, /* a Job that is previewing and has not been streamed yet */

}
