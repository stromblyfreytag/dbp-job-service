package com.trustwave.dbpjobservice.workflow.api.action.valueconverters;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.trustwave.dbpjobservice.workflow.api.action.ValueConverter;

class MapConverter implements ValueConverter {
    private static String Separator = "\b\t";
    private ValueConverterFactory converterFactory;

    public MapConverter(ValueConverterFactory converterFactory) {
        this.converterFactory = converterFactory;
    }

    @Override
    public String getShortString(Object obj) {
        // TODO Auto-generated method stub
        return objectToString(obj);
    }

    @Override
    public String objectToString(Object object) {
        Map<?, ?> map = (Map<?, ?>) object;
        StringBuffer sb = new StringBuffer();
        sb.append('{');

        if (map.size() > 0) {

            Class<?> kClazz = getElementClass(map.keySet());
            ValueConverter kcnv =
                    converterFactory.getConverterFor(kClazz);
            if (kcnv == null) {
                throw new RuntimeException("no converter for key class " + kClazz);
            }
            sb.append(Separator);
            sb.append(kClazz.getName());

            Class<?> vClazz = getElementClass(map.values());
            ValueConverter vcnv =
                    converterFactory.getConverterFor(kClazz);
            if (vcnv == null && vClazz != null) {
                throw new RuntimeException("no converter for value class " + vClazz);
            }
            sb.append(Separator);
            sb.append(vClazz != null ? vClazz.getName() : "");

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sb.append(Separator);
                String skey = kcnv.objectToString(entry.getKey());
                sb.append(skey);

                sb.append(Separator);
                if (entry.getValue() != null) {
                    String vkey = vcnv.objectToString(entry.getValue());
                    sb.append(vkey);
                }
            }
        }
        sb.append(Separator).append('}');
        return sb.toString();
    }

    private Class<?> getElementClass(Collection<?> list) {
        for (Object obj : list) {
			if (obj != null) {
				return obj.getClass();
			}
        }
        return null;
    }

    @Override
    public Object stringToObject(String string, Class<?> type) {
        Map<Object, Object> map = new HashMap<Object, Object>();

        String[] ss = string.split(Separator);
        if (ss.length < 2 || !"{".equals(ss[0]) || !"}".equals(ss[ss.length - 1])) {
            throw new RuntimeException("Not a map string: " + string);
        }
        if (ss.length == 2) {
            return map;
        }

        Class<?> kClazz;
        Class<?> vClazz;
        try {
            kClazz = Class.forName(ss[1]);
            vClazz = (ss[2].length() > 0 ? Class.forName(ss[2]) : null);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e.toString(), e);
        }

        ValueConverter kcnv =
                converterFactory.getConverterFor(kClazz);
        if (kcnv == null) {
            throw new RuntimeException("no converter for key class " + kClazz);
        }
        ValueConverter vcnv =
                converterFactory.getConverterFor(vClazz);
        if (vcnv == null && vClazz != null) {
            throw new RuntimeException("no converter for key class " + kClazz);
        }

        for (int i = 3; i < ss.length - 1; i += 2) {
            Object key = kcnv.stringToObject(ss[i], kClazz);
            Object value = (ss[i + 1].length() > 0 ?
                    vcnv.stringToObject(ss[i + 1], vClazz)
                    : null);
            map.put(key, value);
        }
        return map;
    }

}
