package com.trustwave.dbpjobservice.backend;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="js_job")
public class JobRecord implements Serializable
{
	private static final long serialVersionUID = 1L;

	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="job_id")
	private Long             jobId;
	
	@Column(name="name")
	private String           name;
	
	@Column(name="org_id")
	private int              orgId;
	
	@ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="job_template_id" )
	private JobTemplateRecord jobTemplate;
	
	@Column(name="description")
	private String           description;
	
	@Column(name="utility_job")
	private Boolean 	    utilityJob;
	
	@Column(name="removal_time")
	private Integer 		autoRemoveCompletedInstancesAfterSeconds;

	
    @OneToMany(mappedBy="job", cascade=CascadeType.ALL, fetch=FetchType.LAZY, orphanRemoval=true)
	private Set<JobParameterRecord> parameters = new HashSet<JobParameterRecord>();
    
    @OneToMany(mappedBy="job", cascade=CascadeType.ALL, fetch=FetchType.LAZY)
	private Set<JobProcessRecord> processes = new HashSet<JobProcessRecord>();

    public JobRecord() {
	}
	
	public JobRecord(String name, int orgId, JobTemplateRecord jobTemplate) {
		this.name = name;
		this.jobTemplate = jobTemplate;
		this.orgId = orgId;
		jobTemplate.getJobs().add( this );
	}
	
	/** <p>Add, update, or remove parameter name/value pair.</p>
	 * <p>If value is not null and not empty, parameter name/value pair
	 *  is added or updated.
	 * Otherwise parameter name/value pair is removed from job record.
	 * </p>  
	 * @param pname parameter name
	 * @param value parameter value. If null or empty, parameter is removed.
	 */
	public void addParameter( String pname, String value )
	{
		if (pname == null) {
			throw new IllegalArgumentException( "null parameter name" );
		}
		JobParameterRecord pr = new JobParameterRecord( this, pname, value );
		if (value == null || value.length() == 0) {
			parameters.remove( pr );
		}
		else if (parameters.contains( pr )) {
			for (JobParameterRecord pr0: parameters) {
				if (pr0.equals(pr)) {
					pr0.setValue( value );
					break;
				}
			}
		}
		else {
			parameters.add( pr );
		}
	}
	
	public JobParameterRecord getParameter( String name )
	{
		for (JobParameterRecord pr: parameters) {
			if (name.equals( pr.getParameterName() )) {
				return pr;
			}
		}
		return null;
	}
	
	public Long getJobId() {
		return jobId;
	}
	public void setJobId(Long jobId) {
		this.jobId = jobId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getOrgId() {
		return orgId;
	}
	public void setOrgId(int orgId) {
		this.orgId = orgId;
	}
	public JobTemplateRecord getJobTemplate() {
		return jobTemplate;
	}
	public void setJobTemplate(JobTemplateRecord jobTemplate) {
		this.jobTemplate = jobTemplate;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Set<JobParameterRecord> getParameters() {
		return parameters;
	}
	public void setParameters(Set<JobParameterRecord> parameters) {
		this.parameters = parameters;
	}
	
	public Set<JobProcessRecord> getProcesses()
	{
		return processes;
	}

	public void setProcesses(Set<JobProcessRecord> processes)
	{
		this.processes = processes;
	}

	@Override
	public String toString() {
		return "JobRecord[" + jobId + ", " + name + "@" + orgId + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + orgId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JobRecord other = (JobRecord) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (orgId != other.orgId)
			return false;
		return true;
	}

	public Boolean getUtilityJob() {
		return utilityJob;
	}

	public void setUtilityJob(Boolean utilityJob) {
		this.utilityJob = utilityJob;
	}

	public Integer getAutoRemoveCompletedInstancesAfterSeconds() {
		return autoRemoveCompletedInstancesAfterSeconds;
	}

	public void setAutoRemoveCompletedInstancesAfterSeconds(
			Integer autoRemoveCompletedInstancesAfterSeconds) {
		this.autoRemoveCompletedInstancesAfterSeconds = autoRemoveCompletedInstancesAfterSeconds;
	}

}
