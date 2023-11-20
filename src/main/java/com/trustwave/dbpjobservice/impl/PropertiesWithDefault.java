package com.trustwave.dbpjobservice.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class implements 'two property files' pattern: 
 * optional editable properties file and corresponding default properties file
 * prefixed with 'default.'
 * The folder for both files is assumed to be a part of classpath.
 *  
 * @author VAverchenkov
 *
 */
public class PropertiesWithDefault 
{
	private static Logger logger = LogManager.getLogger( PropertiesWithDefault.class );
	
	private String fileName;
	private String defaultPrefix;
	private Properties properties;
	
	public PropertiesWithDefault( String fileName )
	{
		this( fileName, "default." );
	}

	public PropertiesWithDefault( String fileName, String defaultPrefix )
	{
		this.fileName = fileName;
		this.defaultPrefix = defaultPrefix;
	}

	public synchronized String getProperty( String property )
	{
		if (properties == null) {
			load();
		}
		return properties.getProperty( property );
	}

	public synchronized void setProperty( String property, String value )
	{
		if (properties == null) {
			load();
		}
		properties.setProperty( property, value );
	}

	void load()
	{
		Properties defaults = new Properties();
		String defaultsFile = defaultPrefix + fileName;
		boolean exists = loadProperties( defaults, defaultsFile );
		if (!exists) {
			logger.warn( "No file " + defaultsFile );
			defaults = null;
		}
		
		properties = new Properties( defaults );
		exists = loadProperties( properties, fileName );
		if (!exists && defaults == null) {
			throw new RuntimeException( Messages.getString("props.file.notFound", fileName, defaultsFile) );
		}
	}
	
	boolean loadProperties( Properties props, String fname )
	{
		InputStream stream = getClass().getResourceAsStream( "/" + fname );
		if (stream == null) {
			// resource not found
			return false;
		}
		
		try {
			props.load( stream );
			logger.debug( "Loaded properties from " + fname );
		}
		catch (IOException e) {
			throw new RuntimeException(Messages.getString("props.file.read.error", fname, e), e );
		}
		finally {
			try { stream.close(); } catch (IOException e) {}
		}
		return true;
	}
	
}
