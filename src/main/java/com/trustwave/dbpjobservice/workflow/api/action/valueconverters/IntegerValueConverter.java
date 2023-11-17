package com.trustwave.dbpjobservice.workflow.api.action.valueconverters;

class IntegerValueConverter extends SimpleTypeValueConverter {
    @Override
    public Object stringToObject(String string, Class<?> type) {
        return Integer.valueOf(string);
    }
}
