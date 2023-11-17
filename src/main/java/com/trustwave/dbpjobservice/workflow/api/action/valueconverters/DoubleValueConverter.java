package com.trustwave.dbpjobservice.workflow.api.action.valueconverters;

class DoubleValueConverter extends SimpleTypeValueConverter {
    @Override
    public Object stringToObject(String string, Class<?> type) {
        return Double.valueOf(string);
    }
}
