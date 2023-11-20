package com.trustwave.dbpjobservice.backend;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "js_job_parameters")
@IdClass( JobParameterPK.class )
public class JobParameterRecord implements Serializable
{
	private static final long serialVersionUID = 1L;

	@Id 
	private JobRecord job;
	@Id 
	private String    parameterName;

	@Column(name="value")
	private String value;
	
	
	public JobParameterRecord() {
	}
	
	public JobParameterRecord( JobRecord job, 
			                   String parameterName, String value)
	{
		this.job = job;
		this.parameterName = parameterName;
		this.value = value;
	}

	public JobRecord getJob() {
		return job;
	}
	public void setJob(JobRecord job) {
		this.job = job;
	}
	public String getParameterName() {
		return parameterName;
	}
	public void setParameterName(String parameterName) {
		this.parameterName = parameterName;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((job == null) ? 0 : job.hashCode());
		result = prime * result
				+ ((parameterName == null) ? 0 : parameterName.hashCode());
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
		JobParameterRecord other = (JobParameterRecord) obj;
		if (job == null) {
			if (other.job != null)
				return false;
		} else if (!job.equals(other.job))
			return false;
		if (parameterName == null) {
			if (other.parameterName != null)
				return false;
		} else if (!parameterName.equals(other.parameterName))
			return false;
		return true;
	}
	
}
