package com.trustwave.dbpjobservice.workflow.api.action.valueconverters;

import java.math.BigInteger;

class BigIntegerValueConverter extends SimpleTypeValueConverter {
    @Override
    public Object stringToObject(String string, Class<?> type) {
        return new BigInteger(string);
    }
}
