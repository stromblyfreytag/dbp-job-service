package com.trustwave.dbpjobservice.interfaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.Calendar;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class JobTemplate 
{
	private long                  id;
	private String                name;
	private int                   version;
	private String                description;
	private Calendar              createDate;
	private List<Task>            tasks; 
	private List<ExternalParameterDefinition> parameterDefinitions;
	
	public JobTemplate() {
	}
	
	public JobTemplate(long id, String name, String description) {
		this.id = id;
		this.name = name;
		this.description = description;
	}

	public void setId(long id) {
		this.id = id;
	}
	public long getId() {
		return id;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getDescription() {
		return description;
	}
	public void setParameterDefinitions(List<ExternalParameterDefinition> paramDefs) {
		this.parameterDefinitions = paramDefs;
	}
	public List<ExternalParameterDefinition> getParameterDefinitions() {
		return parameterDefinitions;
	}
	public int getVersion() {
		return version;
	}
	public void setVersion(int version) {
		this.version = version;
	}
	public Calendar getCreateDate() {
		return createDate;
	}
	public void setCreateDate(Calendar createDate) {
		this.createDate = createDate;
	}
	public List<Task> getTasks() {
		return tasks;
	}
	public void setTasks(List<Task> tasks) {
		this.tasks = tasks;
	}

	public ExternalParameterDefinition findParameterDefiniotion( String name )
	{
		for (ExternalParameterDefinition pd: parameterDefinitions) {
			if (pd.getName().equals( name )) {
				return pd;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return "JobTemplate["
		     + "name=" + name
		     + ", id=" + id
			 + ", " + parameterDefinitions
			 + "]";
	}
	
}
