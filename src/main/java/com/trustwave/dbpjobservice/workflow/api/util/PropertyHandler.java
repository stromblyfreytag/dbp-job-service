package com.trustwave.dbpjobservice.workflow.api.util;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

public class PropertyHandler {
    private PropertyDescriptor pd;
    private Class<?> elementClass;

    public PropertyHandler(PropertyDescriptor pd) {
        this.pd = pd;
        this.elementClass = pd.getPropertyType();
        if (isCollectionProperty0()) {
            this.elementClass = getCollectionElementType(getGenetricType());
        }
    }

    static Class<?> getCollectionElementType(Type t) {
        if (t instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) t;
            Type rt = type.getRawType();
            if (rt instanceof Class<?>
                    && Collection.class.isAssignableFrom((Class<?>) rt)) {
                return (Class<?>) type.getActualTypeArguments()[0];
            }
        }
        return null;
    }

    protected PropertyDescriptor getPropertyDescriptor() {
        return pd;
    }

    final boolean isCollectionProperty0() {
        return Collection.class.isAssignableFrom(pd.getPropertyType());
    }

    public boolean isCollectionProperty() {
        return isCollectionProperty0();
    }

    public Class<?> getElementClass() {
        return elementClass;
    }

    public String getPropertyName() {
        return pd.getName();
    }

    public Type getGenetricType() {
        Method m = pd.getReadMethod();
        return (m != null ? m.getGenericReturnType() : pd.getPropertyType());
    }

    public Class<?> getPropertyType() {
        return pd.getPropertyType();
    }

    public Object createProperty(Object obj) {
        try {
            return elementClass.newInstance();
        }
        catch (Exception e) {
            throw new RuntimeException("Cannot create '"
                    + pd.getPropertyType().getName() + "' object "
                    + " for property " + pd.getName() + ": " + e.getMessage());
        }
    }

    public Object getProperty(Object obj) {
        Method getter = pd.getReadMethod();
        if (getter == null) {
            throw new IllegalArgumentException("Property '" + pd.getName()
                    + "' is not readable (no public getter)");
        }
        try {
            return getter.invoke(obj);
        }
        catch (Exception e) {
            throw new RuntimeException("Cannot retrieve value of '" + pd.getName()
                    + "' property: " + e.getMessage(), e);
        }
    }

    public void setProperty(Object obj, Object value) {
        Method setter = pd.getWriteMethod();
        if (setter == null) {
            throw new IllegalArgumentException("Property '" + pd.getName()
                    + "' is not writable (no public setter)");
        }
        try {
            setter.invoke(obj, value);
        }
        catch (Exception e) {
            throw new RuntimeException("Cannot set value of '" + pd.getName()
                    + "' property: " + e.getMessage());
        }
    }

}
