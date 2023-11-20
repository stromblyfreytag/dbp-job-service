package com.trustwave.dbpjobservice.backend;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.googlecode.sarasvati.Graph;
import com.googlecode.sarasvati.hib.HibGraph;

@Entity
@Table(name = "js_job_template")
public class JobTemplateRecord implements Serializable
{
	private static final long serialVersionUID = 1L;

	@Id	@Column(name="job_template_id")
	private Long id;
	
	@OneToOne(fetch=FetchType.LAZY, targetEntity=HibGraph.class)
	@PrimaryKeyJoinColumn
	Graph graph;
	
	@Column(name="template_name", nullable=false)
	private String name;
	
	@Column(name="template_xml", nullable=false)
	private String templateXml;
	
	@Column(name="template_description")
	private String description;

    @OneToMany(mappedBy="jobTemplate", cascade=CascadeType.ALL, fetch=FetchType.LAZY)
	private Set<JobRecord> jobs = new HashSet<JobRecord>();


    public JobTemplateRecord() {
	}
	
	public JobTemplateRecord( Graph graph, 
			                  String templateXml,
			                  String description)
	{
		this.id = ((HibGraph)graph).getId();
		this.graph = graph;
		this.name = graph.getName();
		this.templateXml = templateXml;
		this.description = description;
	}
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Graph getGraph() {
		return graph;
	}
	public void setGraph(Graph graph) {
		this.graph = graph;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getTemplateXml() {
		return templateXml;
	}
	public void setTemplateXml(String templateXml) {
		this.templateXml = templateXml;
	}
	public Set<JobRecord> getJobs() {
		return jobs;
	}
	public void setJobs(Set<JobRecord> jobs) {
		this.jobs = jobs;
	}
	
	public Date getCreateDate()
	{
		return ((HibGraph)graph).getCreateDate();
	}
	
	@Transient
	public String getNameWithVersion()
	{
		return getName() + ", v." + getGraph().getVersion();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		JobTemplateRecord other = (JobTemplateRecord) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}
