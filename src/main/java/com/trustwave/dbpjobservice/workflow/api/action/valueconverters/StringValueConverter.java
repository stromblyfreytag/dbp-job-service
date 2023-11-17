package com.trustwave.dbpjobservice.workflow.api.action.valueconverters;

class StringValueConverter extends SimpleTypeValueConverter {
    @Override
    public Object stringToObject(String string, Class<?> type) {
        return string;
    }
}
