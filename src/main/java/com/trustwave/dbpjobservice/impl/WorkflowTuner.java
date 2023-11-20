package com.trustwave.dbpjobservice.impl;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

public class WorkflowTuner
{
	
	public String tune( String xml )
	{
		return addValidationNode( xml );
	}
	
	public String addValidationNode( String xml )
	{
		try {
			InputStream xsltStream =
					Thread.currentThread().getContextClassLoader()
					      .getResourceAsStream( "add-validation-node.xslt" );
			Source xslt = new StreamSource( xsltStream );
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer(xslt);
			Source input = new StreamSource( new StringReader( xml ) );
			StringWriter output = new StringWriter();
			transformer.transform( input, new StreamResult( output ) );
			return output.toString();
		} 
		catch (Exception e) {
			throw new RuntimeException( Messages.getString("workflow.validation.node.canNotAdd", e.getMessage()), e );
		}
	}
}
