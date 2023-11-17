package com.trustwave.dbpjobservice.workflow.api.util;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;

public class CollectionPropertyHandler extends PropertyHandler {
    public CollectionPropertyHandler(PropertyDescriptor pd) {
        super(pd);
    }

    @Override
    public boolean isCollectionProperty() {
        return true;
    }

    @Override
    public Object getProperty(Object obj) {
        @SuppressWarnings("unchecked")
        Collection<Object> collection = (Collection<Object>) obj;
        if (collection == null) {
            return null;
        }
        Collection<Object> result = new ArrayList<Object>();
        for (Object elem : collection) {
            result.add(super.getProperty(elem));
        }
        return result;
    }

    @Override
    public void setProperty(Object obj, Object value) {
        throw new UnsupportedOperationException();
    }

}
