package com.trustwave.dbpjobservice.workflow.api.util;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.Iterator;

public class KeyValuePairHandler extends PropertyHandler {
    private String keyProperty;
    private String valueProperty;
    private String key;
    private BeanDescriptor beanDescriptor;

    public KeyValuePairHandler(PropertyDescriptor pd,
            String keyProperty,
            String valueProperty,
            String key,
            boolean caseInsensitive) {
        super(pd);
        this.keyProperty = keyProperty;
        this.valueProperty = valueProperty;
        this.key = key;
        this.beanDescriptor =
                new BeanDescriptor(super.getElementClass(), caseInsensitive);
    }

    @Override
    public Object getProperty(Object obj) {
        @SuppressWarnings("unchecked")
        Collection<?> collection = (Collection<Object>) obj;
        for (Object o : collection) {
            Bean b = new Bean(o, beanDescriptor);
            String objKey = b.getPropertyString(keyProperty);
            boolean found = (beanDescriptor.isCaseInsensitive()
                    ? key.equalsIgnoreCase(objKey)
                    : key.equals(objKey));
            if (found) {
                return b.getProperty(valueProperty);
            }
        }
        return null;
    }

    @Override
    public void setProperty(Object obj, Object value) {
        @SuppressWarnings("unchecked")
        Collection<Object> collection = (Collection<Object>) obj;
        Iterator<Object> it = collection.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            Bean b = new Bean(o, beanDescriptor);
            String objKey = b.getPropertyString(keyProperty);
            boolean found = (beanDescriptor.isCaseInsensitive()
                    ? key.equalsIgnoreCase(objKey)
                    : key.equals(objKey));
            if (found) {
                if (value != null) {
                    b.setProperty(valueProperty, value, true);
                }
                else {
                    it.remove();
                }
                return;
            }
        }
        if (value != null) {
            Object o = createProperty(obj);
            Bean b = new Bean(o);
            b.setProperty(valueProperty, value, true);
        }
    }

    public Object createProperty(Object obj) {
        @SuppressWarnings("unchecked")
        Collection<Object> collection = (Collection<Object>) obj;
        Object o = super.createProperty(null);
        Bean b = new Bean(o, beanDescriptor);
        b.setProperty(keyProperty, key, true);
        collection.add(o);
        return o;
    }

    @Override
    public String getPropertyName() {
        return key;
    }

    @Override
    public Class<?> getElementClass() {
        return beanDescriptor.getPropertyType(valueProperty);
    }

}
