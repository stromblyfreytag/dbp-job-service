package com.trustwave.dbpjobservice.backend;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;

/* This is intermediary class to support composite primary key
 * for identifying relationship job-parameter through JPA annotations 
 */
@Embeddable
public class JobParameterPK implements Serializable 
{
	private static final long serialVersionUID = 1L;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="job_id", nullable=false)
	private JobRecord    job;
	
	@Column(name="parameter_name", nullable=false)
	private String       parameterName;

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
		JobParameterPK other = (JobParameterPK) obj;
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
