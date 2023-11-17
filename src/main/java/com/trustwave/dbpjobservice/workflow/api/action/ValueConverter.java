package com.trustwave.dbpjobservice.workflow.api.action;

import com.trustwave.dbpjobservice.workflow.api.action.valueconverters.ValueConverterFactory;

/**
 * <p>Interface for converting objects to strings and back.<p>
 * <p>All action parameter classes must have corresponding value converter.<p>
 * <p>Converters for the following types are predefined or auto-generated:<p>
 * <ul><li>String</li>
 *     <li>Date</li>
 *     <li>primitive types (boolean, int, ...) and corresponding object types</li>
 *     <li>enumeration</li>
 *     <li>List of convertable objects</li>
 *     <li>Map with convertable keys and values</li>
 *     <li>Serializable objects</li>
 * </ul>
 * <p>If parameter's class is not covered with one of the above types,
 *  its converter should be registered with {@link ValueConverterFactory}
 * </p>
 * <p>Parameters without matching value converters will be detected and reported
 * during job template loading.
 * </p>
 */
public interface ValueConverter {
    /**
     * Converts the given object into string format.
     *
     * @param object The object to converter
     * @return String representation of the object.
     */
    public String objectToString(Object object);

    /**
     * Converts string created by {@link #objectToString()} back to object.
     *
     * @param string String representation of the object, usually created by
     * {@link #objectToString()} call.
     * @param type Object type.
     * @return The object represented by the string.
     */
    public Object stringToObject(String string, Class<?> type);

    /**
     * Converts object into a human-readable string.
     * Used for parameter logging only.
     *
     * @param object The object to converter
     * @return Human-readable string corresponding to the object.
     */
    public String getShortString(Object obj);
}
