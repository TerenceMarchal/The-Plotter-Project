package streaming.enums;

/**
 * The available Job pause status
 * 
 * @author Terence
 *
 */
public enum JobPauseStatus {

    ONGOING, /* the Job is not paused */
    CLEANLY_PAUSED, /* the Job has been paused cleanly, meaning it is at a safe height and ready to resume */
    EMERGENCY_PAUSED, /* the Job has been paused by emergency and it might not resume without issues */

}
