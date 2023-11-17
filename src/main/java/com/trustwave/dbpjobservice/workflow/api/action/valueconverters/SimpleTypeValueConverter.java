package com.trustwave.dbpjobservice.workflow.api.action.valueconverters;

import com.trustwave.dbpjobservice.workflow.api.action.ValueConverter;

abstract class SimpleTypeValueConverter implements ValueConverter {
    @Override
    public String objectToString(Object obj) {
        return (obj != null ? obj.toString() : null);
    }

    @Override
    public String getShortString(Object obj) {
        return (obj != null ? obj.toString() : "null");
    }
}
