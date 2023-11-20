package com.trustwave.dbpjobservice.interfaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@XmlAccessorType( XmlAccessType.FIELD )
public class ExternalParameterValue 
{
	/** Parameter name */
	private String  name;
	
	/** Parameter value: first element for single-value parameters,
	 *  list of values for list parameters */
	private List<String> value = new ArrayList<String>();
	
	/** <p>Full definition of the parameter.</p>
	 *  <p>This field will always be set in the parameter value by Job service
	 *   when parameter is sent to client, excluding special case of
	 *   {@link ExternalParameterDefinition} (to avoid infinite recursion).
	 *  </p>
	 *  <p>The field will be ignored when parameter is received from client
	 *   - clients don't need to set it</p>.
	 */
	private ExternalParameterDefinition definition;

	public ExternalParameterValue() {
	}

	public ExternalParameterValue( String name, List<String> value) {
		this.name = name;
		this.value = value;
	}

	public ExternalParameterValue( String name, String ... value) {
		this( name, Arrays.asList( value ) );
	}

	public List<String> getValue() {
		return value;
	}

	public void setValue(List<String> value) {
		this.value = value;
	}

	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	

	public ExternalParameterDefinition getDefinition() {
		return definition;
	}
	public void setDefinition(ExternalParameterDefinition definition) {
		this.definition = definition;
	}

	@Override
	public String toString() 
	{
		String s = String.valueOf( value == null?     null
			                     : value.size() == 0? null
			                     : value.size() == 1? value.get(0)
			                     :                    value        );
		 return name + "=" + hidePassword( s ); 
	}
	
	static String hidePassword( String s )
	{
		if (s == null) {
			return null;
		}
		String s1 = s.replaceAll( "\"attrName\":(\".*?[pP]ass.*?\").*?,\"encrypted\"",
				                  "\"attrName\":$1,****,\"encrypted\"" );
		return s1;
	}
}
