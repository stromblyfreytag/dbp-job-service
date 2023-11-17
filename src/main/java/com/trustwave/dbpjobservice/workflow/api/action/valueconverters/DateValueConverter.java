package com.trustwave.dbpjobservice.workflow.api.action.valueconverters;

import java.util.Date;

class DateValueConverter extends SimpleTypeValueConverter {
    @Override
    public String objectToString(Object obj) {
        return (obj != null ? Long.toString(((Date) obj).getTime()) : null);
    }

    @Override
    public Object stringToObject(String string, Class<?> type) {
        return new Date(Long.valueOf(string));
    }
}
