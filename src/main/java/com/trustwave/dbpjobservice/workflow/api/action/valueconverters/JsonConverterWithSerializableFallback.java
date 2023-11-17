package com.trustwave.dbpjobservice.workflow.api.action.valueconverters;

import com.google.gson.Gson;

/**
 * <p>This converter is used when you want to switch from Serializable converter
 * to JSON converter for an object, keeping compatibility with persisted objects
 * on upgrade.
 * </p>
 * <p>With this converter object may be restored both from old string (serialized)
 * and from json string; conversion to string always produces json string.
 * </p>
 *
 * @author VAverchenkov
 */
public class JsonConverterWithSerializableFallback extends JsonConverter {
    @Override
    public Object stringToObject(String string, Class<?> type) {
        if (string != null && string.startsWith("{\"")) {
            Gson gson = new Gson();
            return gson.fromJson(string, type);
        }
        else {
            return new SerializableConverter().stringToObject(string, type);
        }
    }
}
