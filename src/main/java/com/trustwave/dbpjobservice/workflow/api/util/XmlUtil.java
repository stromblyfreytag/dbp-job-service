package com.trustwave.dbpjobservice.workflow.api.util;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.w3c.dom.Element;

public class XmlUtil {
    public static String toXmlString(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            String pkg = obj.getClass().getPackage().getName();
            JAXBContext jc = JAXBContext.newInstance(pkg);
            Marshaller m = jc.createMarshaller();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            m.marshal(obj, os);
            String xmlString = os.toString();
            return xmlString;
        }
        catch (JAXBException e) {
            throw new RuntimeException("cannot marshall " + obj + ": " + e, e);
        }
    }

    public static String toXmlString(JAXBElement<?> jaxbObj) {
        if (jaxbObj == null || jaxbObj.getValue() == null) {
            return null;
        }
        try {
            String pkg = jaxbObj.getValue().getClass().getPackage().getName();
            JAXBContext jc = JAXBContext.newInstance(pkg);
            Marshaller m = jc.createMarshaller();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            m.marshal(jaxbObj, os);
            String xmlString = os.toString();
            return xmlString;
        }
        catch (JAXBException e) {
            throw new RuntimeException("cannot marshall " + jaxbObj + ": " + e, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromXmlString(String xmlString, Class<T> clazz) {
        if (xmlString == null) {
            return null;
        }
        try {
            JAXBContext jc = JAXBContext.newInstance(clazz.getPackage().getName());
            Unmarshaller u = jc.createUnmarshaller();
            ByteArrayInputStream is = new ByteArrayInputStream(xmlString.getBytes());
            JAXBElement<T> doc = (JAXBElement<T>) u.unmarshal(is);
            return doc.getValue();
        }
        catch (JAXBException e) {
            throw new RuntimeException("cannot unmarshall: " + e, e);
        }
    }

    public static JAXBElement<?> fromDom(Element element, String xmlBindingPackage) {
        try {
            JAXBContext jc = JAXBContext.newInstance(xmlBindingPackage);
            Unmarshaller u = jc.createUnmarshaller();
            return (JAXBElement<?>) u.unmarshal(element);
        }
        catch (JAXBException e) {
            throw new RuntimeException("cannot unmarshall: " + e, e);
        }
    }

    public static QName getQname(Class<?> clazz) {
        XmlType xmlType = clazz.getAnnotation(XmlType.class);
        if (xmlType == null) {
            throw new RuntimeException("Not a JAXB-generated class " + clazz
                    + ": no @XmlType annotation");
        }
        String localName = xmlType.name();
        if (localName == null || localName.equals("##default")) {
            throw new RuntimeException("Not a JAXB-generated class " + clazz
                    + ": no @XmlType.name attribute");
        }
        String namespace = xmlType.namespace();
        if (namespace == null || namespace.equals("##default")) {
            XmlSchema xmlSchema =
                    clazz.getPackage().getAnnotation(XmlSchema.class);
            if (xmlSchema != null) {
                namespace = xmlSchema.namespace();
            }
            if (namespace == null || namespace.equals("")) {
                throw new RuntimeException("Not a JAXB-generated class " + clazz
                        + ": no namespace in @XmlType or package @XmlSchema");
            }
        }
        return new QName(namespace, localName);
    }

    public static boolean isJaxbClass(Class<?> clazz) {
        XmlType t = clazz.getAnnotation(XmlType.class);
        return t != null && t.name() != null && !"##default".equals(t.name());
    }

}
