//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0-b52-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.03.13 at 12:17:21 PM MEZ 
//

package ch.fd.invoice400.response;

import java.math.BigInteger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for errorType complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="errorType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="error" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}integer">
 *             &lt;pattern value="(0|3[0-9]{3})"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="major" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}integer">
 *             &lt;minInclusive value="1000"/>
 *             &lt;maxInclusive value="1999"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="minor" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}integer">
 *             &lt;pattern value="(0|2[0-9]{3})"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "errorType")
public class ErrorType {
	
	@XmlAttribute(required = true)
	protected BigInteger error;
	@XmlAttribute(required = true)
	protected int major;
	@XmlAttribute(required = true)
	protected BigInteger minor;
	
	/**
	 * Gets the value of the error property.
	 * 
	 * @return possible object is {@link BigInteger }
	 * 
	 */
	public BigInteger getError(){
		return error;
	}
	
	/**
	 * Sets the value of the error property.
	 * 
	 * @param value
	 *            allowed object is {@link BigInteger }
	 * 
	 */
	public void setError(BigInteger value){
		this.error = value;
	}
	
	/**
	 * Gets the value of the major property.
	 * 
	 */
	public int getMajor(){
		return major;
	}
	
	/**
	 * Sets the value of the major property.
	 * 
	 */
	public void setMajor(int value){
		this.major = value;
	}
	
	/**
	 * Gets the value of the minor property.
	 * 
	 * @return possible object is {@link BigInteger }
	 * 
	 */
	public BigInteger getMinor(){
		return minor;
	}
	
	/**
	 * Sets the value of the minor property.
	 * 
	 * @param value
	 *            allowed object is {@link BigInteger }
	 * 
	 */
	public void setMinor(BigInteger value){
		this.minor = value;
	}
	
}