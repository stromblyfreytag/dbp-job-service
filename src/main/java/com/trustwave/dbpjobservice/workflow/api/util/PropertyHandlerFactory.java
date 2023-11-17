package com.trustwave.dbpjobservice.workflow.api.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.List;

public class PropertyHandlerFactory {
    private boolean caseInsensitive = false;

    public void addPropertyHandler(Class<?> beanClass,
            String propertyName,
            List<PropertyHandler> chain,
            boolean stickyCollections) {
        PropertyDescriptor pd = getPropertyDescriptor(beanClass, propertyName);
        PropertyHandler handler = new PropertyHandler(pd);
        if (chain.size() > 0 && chain.get(chain.size() - 1).isCollectionProperty()
                && stickyCollections) {
            handler = new CollectionPropertyHandler(pd);
        }
        chain.add(handler);
        if (handler.isCollectionProperty() && !stickyCollections) {
            chain.add(new IndexPropertyHandler(pd));
        }
    }

    protected PropertyDescriptor getPropertyDescriptor(Class<?> beanClazz, String name) {
        BeanInfo bi;
        try {
            bi = Introspector.getBeanInfo(beanClazz);
        }
        catch (IntrospectionException e) {
            throw new IllegalArgumentException("Cannot get beaninfo from " + beanClazz);
        }
        PropertyDescriptor[] pds = bi.getPropertyDescriptors();
        for (PropertyDescriptor pd : pds) {
            if (name.equals(pd.getName())
                    || (caseInsensitive && name.equalsIgnoreCase(pd.getName()))) {
                return pd;
            }
        }
        throw new IllegalArgumentException("No public property " + name
                + " in " + beanClazz);
    }

    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }

    public void setCaseInsensitive(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }
}
