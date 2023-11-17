package com.trustwave.dbpjobservice.workflow.api.action.valueconverters;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.trustwave.dbpjobservice.workflow.api.action.ValueConverter;

public class ListConverterWithElementConverterFallback implements ValueConverter {
    private static Logger logger = LogManager.getLogger(ListConverterWithElementConverterFallback.class);

    private ListConverter listConverter;
    private Class<?> elementType;
    private ValueConverter elementConverter;

    public ListConverterWithElementConverterFallback(
            ListConverter listConverter,
            Class<?> elementType,
            ValueConverter elementConverter) {
        this.listConverter = listConverter;
        this.elementType = elementType;
        this.elementConverter = elementConverter;
    }

    @Override
    public String objectToString(Object object) {
        return listConverter.objectToString(object);
    }

    @Override
    public Object stringToObject(String string, Class<?> type) {
        try {
            return listConverter.stringToObject(string, type);
        }
        catch (NotAListStringException e) {
            logger.debug("List conversion failed: " + e
                    + ", converting as " + elementType);
            Object obj = elementConverter.stringToObject(string, elementType);
            List<Object> list = listConverter.createList();
            list.add(obj);
            return list;
        }
    }

    @Override
    public String getShortString(Object obj) {
        return listConverter.getShortString(obj);
    }

}
