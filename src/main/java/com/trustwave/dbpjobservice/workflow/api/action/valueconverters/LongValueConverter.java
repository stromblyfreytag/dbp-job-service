package com.trustwave.dbpjobservice.workflow.api.action.valueconverters;

class LongValueConverter extends SimpleTypeValueConverter {
    @Override
    public Object stringToObject(String string, Class<?> type) {
        return Long.valueOf(string);
    }
}
