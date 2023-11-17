package com.trustwave.dbpjobservice.workflow.api.action.valueconverters;

import com.google.gson.Gson;
import com.trustwave.dbpjobservice.workflow.api.action.ValueConverter;

public class JsonConverter implements ValueConverter {
    @Override
    public String objectToString(Object obj) {
        Gson gson = new Gson();
        return gson.toJson(obj);
    }

    @Override
    public Object stringToObject(String string, Class<?> type) {
        Gson gson = new Gson();
        return gson.fromJson(string, type);
    }

    @Override
    public String getShortString(Object obj) {
        Gson gson = new Gson();
        return gson.toJson(obj);
    }

}
