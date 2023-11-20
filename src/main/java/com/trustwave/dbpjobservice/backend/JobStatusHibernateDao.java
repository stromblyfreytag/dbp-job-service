package com.trustwave.dbpjobservice.backend;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Component;

@Component
public class JobStatusHibernateDao implements JobStatusDao 
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
	@SuppressWarnings("unchecked")
	public List<UserTaskInfoRecord> getProcessTasks(long processId) 
	{
	    String query = "from UserTaskInfoRecord"
                     + " where processId = :processId"
                     + " order by id";

	    return getSession().createQuery( query )
                           .setLong( "processId", processId )
                           .list();
	}
	@Override
	@SuppressWarnings("unchecked")
	public List<UserTaskInfoRecord> getActiveProcessTasks( long processId )
	{
	    String query = "from UserTaskInfoRecord"
                + " where processId = :processId"
	    		+   " and endTime is null"
                +  " order by id";
	    
	    return getSession().createQuery( query )
                      .setLong( "processId", processId )
                      .list();
	}
	

	@Override
	public UserTaskInfoRecord findTaskInfo( long processId,
			                                String taskName,
			                                String item) 
	{
	    String query = "from UserTaskInfoRecord"
                     + " where processId = :processId"
                     + " and taskName = :taskName"
                     + " and item = :item";
	    
	    return (UserTaskInfoRecord)getSession().createQuery( query )
	                            .setLong(   "processId", processId )
	                            .setString( "taskName",  taskName )
	                            .setString( "item",      item )
	                            .uniqueResult();
	}

	@Override
	public UserTaskInfoRecord findTaskInfo( long taskRecordId )
	{
	    return (UserTaskInfoRecord)getSession()
	    			.get( UserTaskInfoRecord.class, taskRecordId );
	}

	
	@Override
	public void saveTaskInfo(UserTaskInfoRecord utr) 
	{
		getSession().saveOrUpdate( utr );
	}

	@Override
	public void taskCompleted( UserTaskInfoRecord utr )
	{
	}

	@Override
	public Map<String, Long> getAverageActionTimesForJob( long jobId )
	{
		@SuppressWarnings( "unchecked" )
		List<Object[]> records = (List<Object[]>)
				getSession().createSQLQuery( "SELECT node_name, avg_time"
	                                       + "  FROM js_average_node_time_for_job"
				                           + " WHERE job_id = :jobId" )
				                   .setLong( "jobId", jobId )
				                   .list();
		return readAverageTimeRecords( records );
	}
	
	@Override
	public Map<String, Long> getAverageActionTimesForTemplate( long templateId )
	{
		@SuppressWarnings( "unchecked" )
		List<Object[]> records = (List<Object[]>)
				getSession().createSQLQuery( "SELECT node_name, avg_time"
	                                       + "  FROM js_average_node_time_for_template"
				                           + " WHERE template_id = :templateId" )
				                   .setLong( "templateId", templateId )
				                   .list();
		return readAverageTimeRecords( records );
	}
	
	private Map<String, Long> readAverageTimeRecords( List<Object[]> records )
	{
		Map<String, Long> result = new HashMap<String, Long>();
		
		for (Object[] record: records) {
			result.put( (String)record[0], ((Number)record[1]).longValue() );
		}
		return result;
	}
}
