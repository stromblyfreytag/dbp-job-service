package com.trustwave.dbpjobservice.workflow.api.action.valueconverters;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.trustwave.dbpjobservice.workflow.api.action.ValueConverter;
import com.trustwave.dbpjobservice.workflow.api.util.XmlUtil;

public class JaxbConverter implements ValueConverter {
    public JaxbConverter() {
    }

    @Override
    public String objectToString(Object obj) {
        QName qn = XmlUtil.getQname(obj.getClass());
        @SuppressWarnings({"unchecked", "rawtypes"})
        JAXBElement<?> elem = new JAXBElement(qn, obj.getClass(), null, obj);
        return XmlUtil.toXmlString(elem);
    }

    @Override
    public Object stringToObject(String string, Class<?> type) {
        return XmlUtil.fromXmlString(string, type);
    }

    @Override
    public String getShortString(Object obj) {
        return objectToString(obj);
    }

}
