package streaming.enums;

/**
 * The available Plotter status
 * 
 * @author Terence
 *
 */
public enum PlotterStatus {

    DISCONNECTED, /* the Plotter is disconnected */
    IDLE, /* the Plotter is idle and ready to do something */
    RUN, /* the Plotter is executing a Job */
    HOLD, /* the Plotter is on hold (i.e. paused) */
    JOG, /* the Plotter is executing a jog, i.e. a manual movement instruction */
    ALARM, /* an alarm is triggered on the Plotter */
    DOOR, /* a door input is triggered on the Plotter */
    CHECK, /* the Plotter is currently checking (i.e. verifying) instructions */
    HOME, /* the Plotter is currently performing its homing cycle */
    SLEEP, /* the Plotter is currently sleeping */
    SIMULATING,/* the Plotter is a simulating Virtual Plotter */
}
