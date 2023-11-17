package com.trustwave.dbpjobservice.workflow.api.action.valueconverters;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.trustwave.dbpjobservice.workflow.api.action.ValueConverter;

/**
 * <p>The class keeps {@link ValueConverter} objects for converting parameter
 * values to strings and back.</p>
 * <p>Converters for the following types are predefined or auto-generated:
 * <ul><li>String</li>
 *     <li>Date</li>
 *     <li>primitive types (boolean, int, ...) and corresponding object types</li>
 *     <li>enumeration</li>
 *     <li>List of convertable objects</li>
 *     <li>Map with convertable keys and values</li>
 *     <li>Serializable objects</li>
 * </ul>
 * </p>
 * <p>Converters for other types should be explicitly added to this factory, e.g:
 * <pre>
 *    static {
 *       ValueConverterFactory.setConverter( Asset.class, new AssetConverter() );
 *    }
 * </pre>
 * </p>
 * <p>Converter registration should be performed before action parameters are
 * analyzed, typically in a static initialization area of the action class.
 * </p>
 * <p>As a convenience feature, value classes may also specify converter with
 * the static <code>getValueConverter()</code> method, e.g.
 * <pre>
 * public class TimeWindow {
 *     public static ValueConverter getValueConverter() {
 *         return new TimeWindowConverter();
 *     }
 *     ...
 * </pre>
 * Such converters will be auto-detected and registered with factory during
 * parameter analyzing; they don't need to be registered explicitly.
 * </p>
 *
 * @author vlad
 */
public class ValueConverterFactory {
    private static ValueConverterFactory instance = new ValueConverterFactory();
    private Map<Class<?>, ValueConverter> convMap =
            new HashMap<Class<?>, ValueConverter>();

    protected ValueConverterFactory() {
        addConverter(String.class, new StringValueConverter());
        addConverter(Date.class, new DateValueConverter());
        addConverter(Boolean.class, new BooleanValueConverter());
        addConverter(Boolean.TYPE, new BooleanValueConverter());
        addConverter(Long.class, new LongValueConverter());
        addConverter(Long.TYPE, new LongValueConverter());
        addConverter(Integer.class, new IntegerValueConverter());
        addConverter(Integer.TYPE, new IntegerValueConverter());
        addConverter(Double.class, new DoubleValueConverter());
        addConverter(Double.TYPE, new DoubleValueConverter());
        addConverter(BigInteger.class, new BigIntegerValueConverter());

        addConverter(List.class, new ListConverter(this));
        addConverter(ArrayList.class, new ListConverter(this));
        addConverter(LinkedList.class, new LinkedListConverter(this));
        addConverter(Map.class, new MapConverter(this));
        addConverter(Serializable.class, new SerializableConverter());
    }

    /**
     * @return singleton instance of the ValueConverterFactory
     */
    public static ValueConverterFactory getInstance() {
        return instance;
    }

    protected static void setInstance(ValueConverterFactory instance) {
        ValueConverterFactory.instance = instance;
    }

    /**
     * <p>Convenience method for registering converters.</p>
     * <p>Equivalent to
     * <pre>
     *    ValueConverterFactory.getInstance().addConverter( clazz, converter )
     * </pre>
     * </p>
     *
     * @param clazz Class or interface for which converter is registered.
     * @param converter {@link ValueConverter} for objects of this class.
     */
    public static void setConverter(Class<?> clazz, ValueConverter converter) {
        getInstance().addConverter(clazz, converter);
    }

    /**
     * Convenience abbreviation for:
     * <pre>
     * ValueConverterFactory.getInstance().getConverterFor(obj.getClass()).getShortString(obj)
     * </pre>
     *
     * @param obj
     * @return
     */
    public static String objectShortString(Object obj) {
        if (obj == null) {
            return null;
        }
        ValueConverter converter = getInstance().getConverterFor(obj.getClass());
        return (converter != null ? converter.getShortString(obj)
                : obj.toString());
    }

    /**
     * Register converter for the specified class with the factory.
     *
     * @param clazz - Class or interface for which converter is registered.
     * @param converter - {@link ValueConverter} for objects of this class.
     */
    public void addConverter(Class<?> clazz, ValueConverter converter) {
        convMap.put(clazz, converter);
    }

    /**
     * Returns {@link ValueConverter} for objects of the specified class or
     * <code>null</code> if converter for this class cannot be found.
     */
    public ValueConverter getConverterFor(Class<?> clazz) {
        ValueConverter cnv = convMap.get(clazz);
        if (cnv == null && clazz != null) {
            // first try explicitly declared converter
            cnv = getDeclaredValueConverter(clazz);
            // now try generic converters
            if (cnv == null) {
                cnv = getEnumConverter(clazz);
            }
            if (cnv == null) {
                cnv = getInterfaceConverter(clazz);
            }
            if (cnv != null) {
                addConverter(clazz, cnv);
            }
        }
        return cnv;
    }

    public ValueConverter getConverterFor(Class<?> clazz, Class<?> elemType) {
        ValueConverter cnv = getConverterFor(clazz);
        if (cnv instanceof ListConverter && elemType != null) {
            ValueConverter elemConverter = getConverterFor(elemType);
            if (elemConverter != null) {
                cnv = new ListConverterWithElementConverterFallback(
                        (ListConverter) cnv, elemType, elemConverter);
            }
        }
        return cnv;
    }

    private ValueConverter getDeclaredValueConverter(Class<?> clazz) {
        ValueConverter cnv = null;

        for (Method m : clazz.getDeclaredMethods()) {
            if ("getValueConverter".equals(m.getName())
                    && Modifier.isStatic(m.getModifiers())
                    && ValueConverter.class.isAssignableFrom(m.getReturnType())) {
                try {
                    cnv = (ValueConverter) m.invoke(null);
                }
                catch (Exception e) {
                }
            }
        }
        return cnv;
    }

    private ValueConverter getEnumConverter(Class<?> clazz) {
        if (clazz.isEnum()) {
            Method[] methods = clazz.getDeclaredMethods();
            Method mValue = null;
            for (Method m : methods) {
                if ("value".equals(m.getName())
                        && String.class.equals(m.getReturnType())) {
                    mValue = m;
                    break;
                }
            }
            Method mFromValue = null;
            for (Method m : methods) {
                if ("fromValue".equals(m.getName())
                        && clazz.equals(m.getReturnType())
                        && Modifier.isStatic(m.getModifiers())) {
                    mFromValue = m;
                    break;
                }
            }
            if (mValue != null && mFromValue != null) {
                return new JaxbEnumConverter(mValue, mFromValue);
            }
            return new EnumConverter();
        }
        return null;
    }

    private ValueConverter getInterfaceConverter(Class<?> clazz) {
        Class<?>[] interfaces = clazz.getInterfaces();
        for (Class<?> intrf : interfaces) {
            ValueConverter cnv = convMap.get(intrf);
            if (cnv != null) {
                return cnv;
            }
        }
        return null;
    }
}
