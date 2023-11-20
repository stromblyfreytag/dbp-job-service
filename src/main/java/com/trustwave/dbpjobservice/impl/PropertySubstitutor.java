package com.trustwave.dbpjobservice.impl;

import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PropertySubstitutor 
{
	private static Logger logger = LogManager.getLogger( PropertySubstitutor.class );

	private static final String PropertyPrefix = "${property:";
	private static final String PropertySuffix = "}";
	
	private PropertiesWithDefault[] properties;
	// hash of used properties, used for logging only
	private HashSet<String> used = new HashSet<String>();
	
	public PropertySubstitutor( String ... propertyFileNames )
	{
		properties = new PropertiesWithDefault[ propertyFileNames.length ];
		for (int i = 0;  i < properties.length;  i++) {
			properties[i] = new PropertiesWithDefault( propertyFileNames[i] );
		}
	}

	public String substitute( String inputStr )
	{
		StringBuffer sb = new StringBuffer();
		int ibeg = inputStr.indexOf( PropertyPrefix );
		int iend = 0; 
		while (ibeg >= 0) {
			sb.append( inputStr.substring( iend, ibeg ) );
			iend = inputStr.indexOf( PropertySuffix, ibeg + PropertyPrefix.length() );
			if (iend > 0) {
				String property = inputStr.substring( ibeg + PropertyPrefix.length(), iend );
				String value = getPropertyValue( property );
				sb.append( value );
				iend += PropertySuffix.length();
				ibeg = inputStr.indexOf( PropertyPrefix, iend );
			}
			else {
				// unclosed property - break and leave as is
				iend = ibeg;
				break;
			}
		}
		sb.append( inputStr.substring( iend ) );
		
		return sb.toString();
	}
	
	String getPropertyValue( String property )
	{
		String value = null;
		for (int i = 0;  value == null && i < properties.length;  i++) {
			value = properties[i].getProperty( property );
		}
		if (value == null) {
			throw new RuntimeException( "No property '" + property + "'" );
		}
		if (used.add( property )) {
			logger.info( "Substitution: " + property + "=" + value );
		}
		return value;
	}
	
	// used for testing only
	void setProperty( String property, String value )
	{
		properties[0].setProperty( property, value);
	}
}
