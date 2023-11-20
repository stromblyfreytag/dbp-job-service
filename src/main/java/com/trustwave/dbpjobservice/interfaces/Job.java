package com.trustwave.dbpjobservice.interfaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class Job 
{
	private long            id;
	private long            templateId;
	private String          templateName;
	private String          name;
	private int             orgId;
	private String          description;
	private Boolean 	    utilityJob;
	private Integer 		autoRemoveCompletedInstancesAfterSeconds;

	private List<ExternalParameterValue> parameters = new ArrayList<ExternalParameterValue>();
	
	/** List of problems that prevent starting this job -
	 *  e.g. missing parameters, non-externalizable parameters, etc. */
	private List<String>    problems = new ArrayList<String>();
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public long getTemplateId() {
		return templateId;
	}
	public void setTemplateId(long templateId) {
		this.templateId = templateId;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public int getOrgId() {
		return orgId;
	}
	public void setOrgId(int orgId) {
		this.orgId = orgId;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getDescription() {
		return description;
	}
	public List<ExternalParameterValue> getParameters() {
		return parameters;
	}
	public String getTemplateName() {
		return templateName;
	}
	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}
	public List<String> getProblems() {
		return problems;
	}
	public void setUtilityJob(Boolean utilityJob) {
		this.utilityJob = utilityJob;
	}
	public Boolean getUtilityJob() {
		return utilityJob;
	}
	public Integer getAutoRemoveCompletedInstancesAfterSeconds() {
		return autoRemoveCompletedInstancesAfterSeconds;
	}
	public void setAutoRemoveCompletedInstancesAfterSeconds(
			Integer autoRemoveCompletedInstancesAfterSeconds) {
		this.autoRemoveCompletedInstancesAfterSeconds = autoRemoveCompletedInstancesAfterSeconds;
	}
	
	public ExternalParameterValue findParameter( String name )
	{
		for (ExternalParameterValue pv: parameters) {
			if (pv.getName().equals( name )) {
				return pv;
			}
		}
		return null;
	}
	
	public void mergeParameters( List<ExternalParameterValue> additionalParameters )
	{
		for (ExternalParameterValue pv: additionalParameters) {
			ExternalParameterValue pv0 = findParameter( pv.getName() );
			if (pv0 != null) {
				pv0.setValue( pv.getValue() );
			}
			else {
				parameters.add( pv );
			}
		}
	}

	@Override
	public String toString() {
		return "Job[" 
	           + "name=" + name
		       + ", org=" + orgId
		       + ", id=" + id
		       + ", template=" + templateName  + "[id=" + templateId + "]"
		       + "]";
	}

	
}
