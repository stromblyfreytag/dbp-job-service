package com.trustwave.dbpjobservice.workflow.api.action.valueconverters;

import com.trustwave.dbpjobservice.workflow.api.action.ValueConverter;

public class EnumConverter implements ValueConverter {
    @Override
    public String getShortString(Object obj) {
        return obj.toString();
    }

    @Override
    public String objectToString(Object object) {
        return object.toString();
    }

    @Override
    public Object stringToObject(String string, Class<?> type) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Enum<?> enumValue =
                Enum.valueOf((Class<? extends Enum>) type, string);
        return enumValue;
    }

}
