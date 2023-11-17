//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2023.11.08 at 03:34:30 PM CST 
//

package com.trustwave.dbpjobservice.xml;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the com.appsec.jobservice.xml package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _ExtendedDescription_QNAME = new QName("urn:appsecinc.com/js-workflow-extensions", "extendedDescription");
    private final static QName _Attributes_QNAME = new QName("urn:appsecinc.com/js-workflow-extensions", "attributes");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.appsec.jobservice.xml
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link XmlAttributes }
     */
    public XmlAttributes createXmlAttributes() {
        return new XmlAttributes();
    }

    /**
     * Create an instance of {@link XmlExtendedDescription }
     */
    public XmlExtendedDescription createXmlExtendedDescription() {
        return new XmlExtendedDescription();
    }

    /**
     * Create an instance of {@link XmlEventDescriptor }
     */
    public XmlEventDescriptor createXmlEventDescriptor() {
        return new XmlEventDescriptor();
    }

    /**
     * Create an instance of {@link XmlOutput }
     */
    public XmlOutput createXmlOutput() {
        return new XmlOutput();
    }

    /**
     * Create an instance of {@link XmlTimeout }
     */
    public XmlTimeout createXmlTimeout() {
        return new XmlTimeout();
    }

    /**
     * Create an instance of {@link XmlParameters }
     */
    public XmlParameters createXmlParameters() {
        return new XmlParameters();
    }

    /**
     * Create an instance of {@link XmlSelectArc }
     */
    public XmlSelectArc createXmlSelectArc() {
        return new XmlSelectArc();
    }

    /**
     * Create an instance of {@link XmlSelector }
     */
    public XmlSelector createXmlSelector() {
        return new XmlSelector();
    }

    /**
     * Create an instance of {@link XmlFlags }
     */
    public XmlFlags createXmlFlags() {
        return new XmlFlags();
    }

    /**
     * Create an instance of {@link XmlValidator }
     */
    public XmlValidator createXmlValidator() {
        return new XmlValidator();
    }

    /**
     * Create an instance of {@link XmlNameValuePair }
     */
    public XmlNameValuePair createXmlNameValuePair() {
        return new XmlNameValuePair();
    }

    /**
     * Create an instance of {@link XmlActionType }
     */
    public XmlActionType createXmlActionType() {
        return new XmlActionType();
    }

    /**
     * Create an instance of {@link XmlParameter }
     */
    public XmlParameter createXmlParameter() {
        return new XmlParameter();
    }

    /**
     * Create an instance of {@link XmlTask }
     */
    public XmlTask createXmlTask() {
        return new XmlTask();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XmlExtendedDescription }{@code >}}
     */
    @XmlElementDecl(namespace = "urn:appsecinc.com/js-workflow-extensions", name = "extendedDescription")
    public JAXBElement<XmlExtendedDescription> createExtendedDescription(XmlExtendedDescription value) {
        return new JAXBElement<XmlExtendedDescription>(_ExtendedDescription_QNAME, XmlExtendedDescription.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XmlAttributes }{@code >}}
     */
    @XmlElementDecl(namespace = "urn:appsecinc.com/js-workflow-extensions", name = "attributes")
    public JAXBElement<XmlAttributes> createAttributes(XmlAttributes value) {
        return new JAXBElement<XmlAttributes>(_Attributes_QNAME, XmlAttributes.class, null, value);
    }

}