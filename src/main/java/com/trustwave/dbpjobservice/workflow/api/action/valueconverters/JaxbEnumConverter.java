package com.trustwave.dbpjobservice.workflow.api.action.valueconverters;

import java.lang.reflect.Method;

import com.trustwave.dbpjobservice.workflow.api.action.ValueConverter;

public class JaxbEnumConverter implements ValueConverter {
    private Method mValue;
    private Method mFromValue;

    public JaxbEnumConverter(Method mValue, Method mFromValue) {
        this.mValue = mValue;
        this.mFromValue = mFromValue;
    }

    @Override
    public String getShortString(Object obj) {
        return objectToString(obj);
    }

    @Override
    public String objectToString(Object obj) {
        try {
            return (String) mValue.invoke(obj);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Cannot convert " + obj
                    + " to string: " + e);
        }
    }

    @Override
    public Object stringToObject(String string, Class<?> type) {
        try {
            return mFromValue.invoke(null, string);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Cannot convert " + string
                    + " to " + type + ": " + e);
        }
    }

}
