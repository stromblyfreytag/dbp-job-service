//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2023.11.08 at 03:34:30 PM CST 
//

package com.trustwave.dbpjobservice.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

/**
 * <p>Java class for XmlActionType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="XmlActionType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="class" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="mainstream" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       &lt;attribute name="expectedTime" type="{http://www.w3.org/2001/XMLSchema}int" default="1" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "XmlActionType")
public class XmlActionType
        implements Serializable {

    private final static long serialVersionUID = 1L;
    @XmlAttribute(name = "class", required = true)
    protected String clazz;
    @XmlAttribute(name = "mainstream")
    protected Boolean mainstream;
    @XmlAttribute(name = "expectedTime")
    protected Integer expectedTime;

    /**
     * Gets the value of the clazz property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getClazz() {
        return clazz;
    }

    /**
     * Sets the value of the clazz property.
     *
     * @param value allowed object is
     * {@link String }
     */
    public void setClazz(String value) {
        this.clazz = value;
    }

    /**
     * Gets the value of the mainstream property.
     *
     * @return possible object is
     * {@link Boolean }
     */
    public boolean isMainstream() {
        if (mainstream == null) {
            return false;
        }
        else {
            return mainstream;
        }
    }

    /**
     * Sets the value of the mainstream property.
     *
     * @param value allowed object is
     * {@link Boolean }
     */
    public void setMainstream(Boolean value) {
        this.mainstream = value;
    }

    /**
     * Gets the value of the expectedTime property.
     *
     * @return possible object is
     * {@link Integer }
     */
    public int getExpectedTime() {
        if (expectedTime == null) {
            return 1;
        }
        else {
            return expectedTime;
        }
    }

    /**
     * Sets the value of the expectedTime property.
     *
     * @param value allowed object is
     * {@link Integer }
     */
    public void setExpectedTime(Integer value) {
        this.expectedTime = value;
    }

}
