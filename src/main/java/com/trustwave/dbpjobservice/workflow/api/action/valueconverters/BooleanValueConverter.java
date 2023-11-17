package com.trustwave.dbpjobservice.workflow.api.action.valueconverters;

class BooleanValueConverter extends SimpleTypeValueConverter {
    @Override
    public Object stringToObject(String string, Class<?> type) {
        String s = (string != null ? string.toLowerCase() : "");
        if ("y".equals(s) || "yes".equals(s) || "t".equals(s) || "true".equals(s) || "1".equals(s)) {
            return true;
        }
        if ("n".equals(s) || "no".equals(s) || "f".equals(s) || "false".equals(s) || "0".equals(s)) {
            return false;
        }
        throw new IllegalArgumentException("Not a boolean value: " + string);
    }
}
