package com.trustwave.dbpjobservice.backend;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Component;

@Component
class JobHibernateDao implements JobDao 
{
	private SessionFactory sessionFactory;
	
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	protected Session getSession()
	{
		return sessionFactory.getCurrentSession();
	}

	@Override
	public void deleteJob(JobRecord job ) 
	{
		getSession().delete( job );
	}

	@Override
	public void deleteJobTemplate( JobTemplateRecord jobTemplate ) 
	{
		getSession().delete( jobTemplate );
	}

	@Override
	public JobTemplateRecord findJobTemplate( Long graphId ) 
	{
		return (JobTemplateRecord)getSession().get(	JobTemplateRecord.class, graphId );
	}

	@Override
	public JobTemplateRecord findJobTemplate( String name ) 
	{
	    String query = "from JobTemplateRecord jtr"
	                 + " where name= :name and graph.version = "
                     + "(select max(version) from HibGraph" 
                     + "  where name = jtr.graph.name)";
	    return (JobTemplateRecord)getSession().createQuery( query )
	                                          .setString( "name", name )
	                                          .uniqueResult();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<JobTemplateRecord> getJobTemplates() 
	{
	    String query = "from JobTemplateRecord jtr"
	    	         + " where graph.version = "
	                 + "(select max(version) from HibGraph" 
	                 + "  where name = jtr.graph.name)";

	    return getSession().createQuery( query ).list();
	}

	@Override
	public void saveJobTemplate(JobTemplateRecord jobTemplate) 
	{
		getSession().saveOrUpdate( jobTemplate );
	}
	
	@SuppressWarnings("unchecked")
	public List<JobTemplateRecord> getAllJobTemplates() 
	{
		Criteria crit = getSession().createCriteria( JobTemplateRecord.class );
		return crit.list();
	}
	
	@Override
	public void saveJob(JobRecord job) 
	{
		getSession().saveOrUpdate( job );
	}

	@Override
	public JobRecord findJob(long jobId) 
	{
		return (JobRecord)getSession().get( JobRecord.class, jobId );
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<JobRecord> findJobsByTemplateId( long templateId )
	{
		String query = "from JobRecord where jobTemplate.id = :templateId";
		return getSession().createQuery( query )
		                   .setLong( "templateId", templateId )
		                   .list();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<JobRecord> findAllJobs()
	{
		return getSession().createCriteria( JobRecord.class ).list();
	}
	
	
	@Override
	public JobRecord findJob( String jobName, int orgId )
	{
	    String query = "from JobRecord where name= :name and orgId=:orgId";
	    return (JobRecord)getSession().createQuery( query )
                                     .setString( "name", jobName )
                                     .setInteger( "orgId", orgId )
                                     .uniqueResult();
	}
	
}
