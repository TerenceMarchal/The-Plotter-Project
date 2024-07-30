package common.engine;

import org.json.JSONObject;

/**
 * An interface for exporting and importing instance as JSON objects
 * 
 * @author Terence
 *
 */
public interface Jsonizable {

    /**
     * Generate a JSONObject representing this Jsonizable instance
     * 
     * @return a JSONObject representing this Jsonizable instance
     */
    public JSONObject toJSonObject();

    /**
     * Create a Jsonizable instance from the specified JSONObject
     * 
     * @param jsonObject the JSONObject from which to create a Jsonizable instance
     * @return the generated Jsonizable instance
     */
    public static Jsonizable fromJsonObject(JSONObject jsonObject) {
        return null;
    }

}
