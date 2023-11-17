package com.trustwave.dbpjobservice.workflow.api.action.valueconverters;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.trustwave.dbpjobservice.workflow.api.action.ValueConverter;

class ListConverter implements ValueConverter {
    private static String Separator = "\b\f";
    private ValueConverterFactory converterFactory;

    public ListConverter(ValueConverterFactory converterFactory) {
        this.converterFactory = converterFactory;
    }

    @Override
    public String getShortString(Object obj) {
        // TODO Auto-generated method stub
        return objectToString(obj);
    }

    @Override
    public String objectToString(Object object) {
        List<?> list = (List<?>) object;
        StringBuffer sb = new StringBuffer();
        sb.append('[');

        if (list.size() > 0) {
            sb.append(Separator);
            Class<?> clazz = list.get(0).getClass();
            sb.append(clazz.getName());
            ValueConverter cnv =
                    converterFactory.getConverterFor(clazz);
            if (cnv == null) {
                throw new RuntimeException("no converter for class " + clazz);
            }

            for (Object obj : list) {
                sb.append(Separator);
                String s = cnv.objectToString(obj);
                sb.append(s);
            }
        }
        sb.append(Separator).append(']');
        return sb.toString();
    }

    protected List<Object> createList() {
        return new ArrayList<Object>();
    }

    @Override
    public Object stringToObject(String string, Class<?> type) {
        List<Object> list = createList();

        String[] ss = string.split(Separator);
        if (ss.length < 2 || !"[".equals(ss[0]) || !"]".equals(ss[ss.length - 1])) {
            throw new NotAListStringException("Not a list string: \"" + string + "\"");
        }
        if (ss.length > 2) {
            Class<?> clazz;
            try {
                clazz = Class.forName(ss[1]);
            }
            catch (ClassNotFoundException e) {
                throw new RuntimeException(e.toString(), e);
            }
            ValueConverter cnv =
                    converterFactory.getConverterFor(clazz);
            if (cnv == null) {
                throw new RuntimeException("no converter for class " + clazz);
            }

            for (int i = 2; i < ss.length - 1; i++) {
                Object obj = cnv.stringToObject(ss[i], clazz);
                list.add(obj);
            }
        }
        return list;
    }

}

class LinkedListConverter extends ListConverter {
    public LinkedListConverter(ValueConverterFactory converterFactory) {
        super(converterFactory);
    }

    @Override
    protected List<Object> createList() {
        return new LinkedList<Object>();
    }

}
