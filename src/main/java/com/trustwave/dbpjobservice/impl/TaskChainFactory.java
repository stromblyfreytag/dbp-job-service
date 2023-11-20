package com.trustwave.dbpjobservice.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;










import com.googlecode.sarasvati.Graph;
import com.googlecode.sarasvati.Node;
import com.trustwave.dbpjobservice.backend.JobDao;
import com.trustwave.dbpjobservice.backend.JobProcessRecord;
import com.trustwave.dbpjobservice.backend.JobStatusDao;
import com.trustwave.dbpjobservice.backend.JobTemplateRecord;
import com.trustwave.dbpjobservice.interfaces.Task;
import com.trustwave.dbpjobservice.interfaces.TaskSummary;
import com.trustwave.dbpjobservice.workflow.JobActionNode;
import com.trustwave.dbpjobservice.workflow.WorkflowUtils;
import com.trustwave.dbpjobservice.workflow.api.util.LruCache;
import com.trustwave.dbpjobservice.xml.XmlActionType;
import com.trustwave.dbpjobservice.xml.XmlTask;

public class TaskChainFactory 
{
	private final static Logger logger = LogManager.getLogger( TaskChainFactory.class );
	
	private JobDao jobDao;
	private JobStatusDao statusDao;
	private Configuration config;
	private Map<Long, TaskChain> allTaskChains;
	private Map<Long, TaskChain> processIdMap;
	
	public void init()
	{
		// 20 - expected max number of active workflow templates
		allTaskChains = new LruCache<>( 20 );
		// process map is critical for performance (populating entry requires
		// re-reading from heavy database views), let's be generous for it:
		processIdMap = new LruCache<>( config.getProcessCacheSize() * 2 );
	}
	
	public synchronized TaskChain getTaskChain( JobTemplateRecord jtr )
	{
		TaskChain tc = allTaskChains.get( jtr.getId() );
		if (tc == null) {
			tc = create( jtr );
			allTaskChains.put( jtr.getId(), tc );
		}
		return tc;
	}
	
	public synchronized TaskChain getTaskChain( long templateId )
	{
		TaskChain tc = allTaskChains.get( templateId );
		if (tc == null) {
			JobTemplateRecord jtr = jobDao.findJobTemplate( templateId );
			tc = create( jtr );
			allTaskChains.put( jtr.getId(), tc );
		}
		return tc;
	}
	
	public synchronized TaskChain getTaskChainByProcess( JobProcessRecord jpr )
	{
		TaskChain tc = processIdMap.get( jpr.getId() );
		if (tc == null) {
			tc = new TaskChain( getTaskChain( jpr.getTemplateId() ) );
			Map<String, Long> timesMap;
			if (config.isUseAverageTimeForPercentage()) {
				timesMap = getAverageActionTimeMap( jpr );
			}
			else {
				timesMap = getExpectedActionTimeMap( tc );
			}
			tc.initTaskTimes( timesMap );
			processIdMap.put( jpr.getId(), tc );
		}
		return tc;
	}
	
	Map<String, Long> getAverageActionTimeMap( JobProcessRecord jpr )
	{
		Map<String, Long> averageActionTime = null;
		try {
			averageActionTime = statusDao.getAverageActionTimesForJob( jpr.getJob().getJobId() );
		}
		catch (Exception e) {
			logger.warn( "Cannot retrieve average times for job "
		               + jpr.getJob().getJobId() + ": " + e, e );
		}
		if (averageActionTime == null || averageActionTime.isEmpty()) {
			try {
				averageActionTime = 
						statusDao.getAverageActionTimesForTemplate( jpr.getTemplateId() );
			}
			catch (Exception e) {
				logger.warn( "Cannot retrieve average times for template "
			               + jpr.getTemplateId() + ": " + e, e );
				// we should not return null, anyway:
				averageActionTime = new HashMap<String, Long>();
			}
		}
		return averageActionTime;
	}
	
	Map<String, Long> getExpectedActionTimeMap( TaskChain tc )
	{
		Map<String, Long> expectedActionTime = new HashMap<>();
		for (Task task: tc.getTasks()) {
			for (String actionName: task.getActionNames()) {
				int expectedTime = task.getExpectedTime( actionName );
				if (expectedTime >= 0) {
					// expected time is in seconds, convert to ms 
					expectedActionTime.put( actionName, expectedTime * 1000L );
				}
			}
		}
		return expectedActionTime;
	}
	public synchronized TaskChain updateTaskChain( JobTemplateRecord jtr )
	{
		allTaskChains.remove( jtr.getId() );
		return getTaskChain( jtr );
	}
	
	public synchronized void reset()
	{
		allTaskChains.clear();
	}
	
	TaskChain create( JobTemplateRecord jtr )
	{
		TaskChain chain = new TaskChain(); 
		Graph graph = jtr.getGraph();
		List<Node> linearized =  WorkflowUtils.linearizeGraph( graph );
		TaskSummary lastTs = null;
		
		for (Node n: linearized) {
			if (!(n instanceof JobActionNode)) {
				continue;
			}
			
			final XmlTask xt = ((JobActionNode)n).getAttributes().getTask();
			if (lastTs == null) {
				// first task in template:
				lastTs = chain.addTask( xt != null? xt.getName(): null,
						                xt != null? xt.getCategory(): null,
						                xt != null? xt.isEssential() : false );
			}
			else if (xt != null && !xt.getName().equals( lastTs.getTask().getName() )) {
				// new task action, different from the previous one:
				lastTs = chain.addTask( xt.getName(), 
						                xt.getCategory(),
						                xt.isEssential() );
			}
			else {
				// same task - explicit or implied
			}
			
			XmlActionType type = ((JobActionNode)n).getAttributes().getActionType();
			int expectedTime = (type != null? type.getExpectedTime()
					                        : new XmlActionType().getExpectedTime());
			// add action name and time
			lastTs.getTask().addAction( n.getName(), expectedTime );
		}
		return chain;
	}

	public void setJobDao(JobDao jobDao) {
		this.jobDao = jobDao;
	}
	public void setStatusDao( JobStatusDao jobStatusDao ) {
		this.statusDao = jobStatusDao;
	}
	public void setConfig( Configuration config ) {
		this.config = config;
	}
	
}
