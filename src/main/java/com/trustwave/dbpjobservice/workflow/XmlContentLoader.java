package com.trustwave.dbpjobservice.workflow;

import javax.xml.bind.JAXBException;
import java.io.Reader;
import java.io.StringReader;


import com.googlecode.sarasvati.load.SarasvatiLoadException;
import com.googlecode.sarasvati.xml.XmlLoader;
import com.googlecode.sarasvati.xml.XmlProcessDefinition;
import com.trustwave.dbpjobservice.impl.Messages;

public class XmlContentLoader extends XmlLoader
{
	public XmlProcessDefinition translateContent (final String content) throws SarasvatiLoadException
	{
		return loadProcessDefinition( content );
	}

	private XmlProcessDefinition loadProcessDefinition (final String content)
		throws SarasvatiLoadException
	{
		XmlProcessDefinition def = null;
		try {
			Reader reader = new StringReader( fixContent(content) );
			def = (XmlProcessDefinition) getUnmarshaller().unmarshal( reader );
		}
		catch(JAXBException e) {
			throw new SarasvatiLoadException(Messages.getString("workflow.unmarshal.error"), e);
		}
		return def;
	}
	
	public static String fixContent( String content )
	{
		// 'tokenSetAnd' was reoplaced with 'tokenSet' in Sarasvati 2.0.1:
		return content.replaceAll( "([\"'])tokenSetAnd\\1", "$1tokenSet$1" );
		
	}
}
