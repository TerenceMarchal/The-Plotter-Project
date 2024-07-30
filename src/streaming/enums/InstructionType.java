package streaming.enums;

/**
 * The available Instruction types
 * 
 * @author Terence
 *
 */
public enum InstructionType {

    // TODO-023: we should use subclass of Instruction instead of this enum
    FAST_LINEAR_MOVEMENT, LOADED_LINEAR_MOVEMENT, TOOL_CHANGE, SET_FEED_RATE, USE_ABSOLUTE_COORDINATES,
    USE_RELATIVE_COORDINATES, USE_INCHES_UNITS, USE_MM_UNITS, JOG, HOME, UNLOCK, RESET, FEED_HOLD, FEED_RESUME,
    READ_CONFIGURATION, COMMENT, TOOL_CHANGE_NOTIFICATION, UNKNOWN;

}
