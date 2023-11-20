package com.trustwave.dbpjobservice.backend;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Component;

@Component
public class JobProcessHibernateDao implements JobProcessDao 
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

	public void saveJobProcess( JobProcessRecord jpr )
	{
		getSession().saveOrUpdate( jpr );
	}

	@Override
	public JobProcessRecord getProcess( long id )
	{
		return (JobProcessRecord)getSession().get( JobProcessRecord.class, id );
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public List<JobProcessRecord> getRunningProcesses()
	{
	    String query = "from JobProcessRecord "
	                 + " where completeDate is null" +
	                   " order by createDate";
	    return getSession().createQuery( query ).list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<JobProcessRecord> getAllProcesses()
	{
	    String query = "from JobProcessRecord "
	                 + " order by createDate";
	    return getSession().createQuery( query ).list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<JobProcessRecord> getProcessesForJob( long jobId )
	{
	    String query = "from JobProcessRecord "
                     + " where job.id = :jobId"
                     + " order by createDate";
		return getSession().createQuery( query )
		                   .setLong("jobId",jobId )
		                   .list();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<JobProcessRecord> getCompletedProcesses( Date from, Date to )
	{
		// we need to use Convert() since MSSQL-JDBC 6.x setTimestamp uses datetime2 and jTDS used datetime.
		// this causes an issue with unpredicatble results, such as in this WHERE clause. 
		// the actual MSSQL DB type for these from/to fields is a DATETIME.
		String query = "from JobProcessRecord "
					 + " where completeDate >= Convert(datetime, :from)"
					 + " and completeDate < Convert(datetime, :to)"
					 + " order by createDate";
		return getSession().createQuery( query )
							.setTimestamp( "from", from )
							.setTimestamp( "to",   to )
							.list();
	}


	@SuppressWarnings( "unchecked" )
	@Override
	public Map<Long,ProcessTaskCountsRecord> getActiveTaskCounts()
	{
		Session session = getSession();
		List<ProcessTaskCountsRecord> list =
				(List<ProcessTaskCountsRecord>)
					session.getNamedQuery( "taskCounts" )
					.setReadOnly( true )
					.setCacheMode( CacheMode.IGNORE )
					.list();
		
		Map<Long,ProcessTaskCountsRecord> map = new HashMap<>();
		for (ProcessTaskCountsRecord record: list) {
			map.put( record.getProcessId(), record );
			// ensure next time object will be re-read from query  
			// rather than retrieved from the first-level cache
			// (cache mode above is only for second-level cache, if present):
			session.evict( record );
		}
		return map;
	}

	@Override
	public void deleteProcess( JobProcessRecord jpr )
	{
		getSession().delete(jpr);
	}
}
