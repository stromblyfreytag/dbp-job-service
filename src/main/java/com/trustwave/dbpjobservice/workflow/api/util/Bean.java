package com.trustwave.dbpjobservice.workflow.api.util;

import java.util.List;

import com.trustwave.dbpjobservice.workflow.api.action.ValueConverter;
import com.trustwave.dbpjobservice.workflow.api.action.valueconverters.ValueConverterFactory;

/**
 * <p>Class serves as a wrapper for concrete bean objects to allow access to
 * bean fields by name.</p>
 * <p>Example:</p>
 * <pre><code>
 *     Asset asset = ...
 *     Bean assetBean = new Bean(asset);
 *     String type = assetBean.getProperty("type.name");
 *     assetBean.setProperty("platform.name", "Microsoft Windows 9 Enterprise");
 * </code></pre>
 *
 * @author vlad
 */
public class Bean {
    private Object object;
    private BeanDescriptor descriptor;

    public Bean(Object object) {
        this.object = object;
        descriptor =
                BeanDescriptorFactory.getInstance().getDescriptor(object.getClass());
    }

    public Bean(Object object, BeanDescriptor descriptor) {
        this.object = object;
        this.descriptor = descriptor;
    }

    public static Bean createFromString(String s, Class<?> clazz) {
        Object obj = stringToObject(s, clazz);
        BeanDescriptor descriptor =
                BeanDescriptorFactory.getInstance().getDescriptor(clazz);
        return new Bean(obj, descriptor);
    }

    private static Object stringToObject(String str, Class<?> clazz) {
        if (str == null) {
            return null;
        }
        ValueConverter converter =
                ValueConverterFactory.getInstance().getConverterFor(clazz);
        if (converter == null) {
            throw new RuntimeException("No ValueConverter for " + clazz);
        }
        return converter.stringToObject(str, clazz);
    }

    public Object getObject() {
        return object;
    }

    public BeanDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * Check if bean has declared property with the specified name
     *
     * @param propertyName property name
     * @return <code>true</code> if bean has declared property with the specified
     * name, <code>false</code> otherwise.
     */
    public boolean hasDeclaredProperty(String propertyName) {
        return descriptor.hasDeclaredProperty(propertyName);
    }

    /**
     * Get property value by property name
     *
     * @param propertyName
     * @return property value, <code>null</code> if target or intermediate property
     * was not set.
     * @throws IllegalArgumentException when property name is invalid (does not
     * correspond to one of the declared bean properties)
     */
    public Object getProperty(String propertyName) {
        List<PropertyHandler> phlist =
                descriptor.getPropertyHandlers(propertyName);

        Object obj = object;
        for (int level = 0; level < phlist.size() && obj != null; ++level) {
            PropertyHandler ph = phlist.get(level);
            obj = ph.getProperty(obj);
        }
        return obj;
    }

    /**
     * <p>Get property value string by property name.</p>
     * <p>Same as {@link #getProperty(String)}, retrieved value is converted to
     * string with corresponding {@link ValueConverter}.</p>
     *
     * @param propertyName
     * @return property value string, <code>null</code> if target or intermediate
     * property was not set.
     * @throws IllegalArgumentException when property name is invalid (does not
     * correspond to one of the declared bean properties),
     * or there is no value converter for property class.
     */
    public String getPropertyString(String propertyName) {
        Object obj = getProperty(propertyName);
        if (obj == null) {
            return null;
        }
        ValueConverter converter =
                ValueConverterFactory.getInstance().getConverterFor(obj.getClass());
        if (converter == null) {
            throw new IllegalArgumentException("No ValueConverter for " + obj.getClass());
        }
        return converter.objectToString(obj);
    }

    /**
     * Set property value by property name, with optional creation of
     * intermediate properties.
     *
     * @param propertyName property name
     * @param propertyValue value to set
     * @param createIntermediate If <code>true</code>, intermediate properties
     * will be created if necessary.
     * If <code>false</code> and null property object
     * is encountered in the path specified by propertyName, exception
     * will be thrown.
     */
    public void setProperty(String propertyName,
            Object propertyValue,
            boolean createIntermediate) {
        List<PropertyHandler> phlist =
                descriptor.getPropertyHandlers(propertyName);

        // first we need to retrieve object which property is set - retrieve
        // property objects starting from top, until only the last remains.
        Object obj = object;
        PropertyHandler ph = phlist.remove(0);
        while (phlist.size() > 0) {
            Object obj1 = ph.getProperty(obj);
            if (obj1 == null) {
                if (createIntermediate) {
                    obj1 = ph.createProperty(obj);
                    ph.setProperty(obj, obj1);
                }
                else {
                    throw new RuntimeException("Cannot set value of '"
                            + propertyName + "' property: "
                            + ph.getPropertyName() + " is null");
                }
            }
            obj = obj1;
            ph = phlist.remove(0);
        }

        // the last property object is the one which value is set:
        ph.setProperty(obj, propertyValue);
    }

    public void setPropertyString(String propertyName, String valueStr,
            boolean createImtermediate) {
        List<PropertyHandler> phlist =
                descriptor.getPropertyHandlers(propertyName);
        // convert string to object type of the last property in the list:
        PropertyHandler ph = phlist.get(phlist.size() - 1);
        Object propertyValue = stringToObject(valueStr, ph.getElementClass());

        setProperty(propertyName, propertyValue, createImtermediate);
    }

    /**
     * <p>Get current index of the specified collection property;
     * i.e index implied when retrieving/setting properties of collection elements.</p>
     *
     * @param propertyName
     * @return
     */
    public int getIndex(String propertyName) {
        return descriptor.getCollectionIndex(propertyName).getIndex();
    }

    /**
     * <p>Set current index of the specified collection property;
     * i.e index implied when retrieving/setting properties of collection elements.</p>
     *
     * @param propertyName
     */
    public void setIndex(String propertyName, int index) {
        descriptor.getCollectionIndex(propertyName).setIndex(index);
    }

    /**
     * <p>Increase current index of the specified collection property;
     * i.e index implied when retrieving/setting properties of collection elements.</p>
     *
     * @param propertyName
     */
    public void increaseIndex(String propertyName) {
        descriptor.getCollectionIndex(propertyName).increaseIndex();
    }
}
