package com.trustwave.dbpjobservice.impl;





import static com.trustwave.dbpjobservice.interfaces.DetailedInfoScope.AllTasks;
import static com.trustwave.dbpjobservice.interfaces.DetailedInfoScope.None;
import static com.trustwave.dbpjobservice.interfaces.JobProcessInfo.CompletionState.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;















import com.googlecode.sarasvati.GraphProcess;
import com.googlecode.sarasvati.Node;
import com.trustwave.dbpjobservice.backend.JobDao;
import com.trustwave.dbpjobservice.backend.JobProcessRecord;
import com.trustwave.dbpjobservice.backend.JobStatusDao;
import com.trustwave.dbpjobservice.backend.JobTemplateRecord;
import com.trustwave.dbpjobservice.backend.ProcessTaskCountsRecord;
import com.trustwave.dbpjobservice.backend.UserTaskInfoRecord;
import com.trustwave.dbpjobservice.interfaces.DetailedInfoScope;
import com.trustwave.dbpjobservice.interfaces.JobProcessInfo;
import com.trustwave.dbpjobservice.interfaces.Task;
import com.trustwave.dbpjobservice.interfaces.TaskInfo;
import com.trustwave.dbpjobservice.interfaces.TaskSummary;
import com.trustwave.dbpjobservice.workflow.EngineFactory;
import com.trustwave.dbpjobservice.workflow.EventManager;
import com.trustwave.dbpjobservice.workflow.JobActionNode;
import com.trustwave.dbpjobservice.workflow.api.util.LruCache;
import com.trustwave.dbpjobservice.xml.XmlAttributes;

/**
 * The class is responsible for retrieving process status from persistent
 * storage (Hibernate objects).
 * Population of Hibernate objects is performed in JobStatusManager.
 * 
 * @author vlad
 *
 */
public class JobStatusRetriever 
{
	private TaskChainFactory taskChainFactory;
	private JobStatusDao statusDao;
	private JobDao jobDao;
	private EngineFactory engineFactory;
	
	public JobProcessInfo getProcessInfo( final JobProcessRecord jpr, final boolean detailed )
	{
		return getProcessInfo( jpr, detailed? AllTasks: None, null );
	}
	
	public JobProcessInfo getProcessInfo( JobProcessRecord jpr,
			                              DetailedInfoScope detailsScope,
			                              ProcessTaskCountsRecord taskCounts)
	{
		if (jpr == null) {
			return null;
		}
		final JobProcessInfo processInfo = new JobProcessInfo();
		processInfo.setProcessId( jpr.getId() );
		processInfo.setJobId( jpr.getJob().getJobId() );
		processInfo.setOrgId(jpr.getJob().getOrgId());
		processInfo.setJobName( jpr.getJobName() );
		int templateId = jpr.getTemplateId();
		processInfo.setTemplateId( templateId );
		processInfo.setTemplateName( getTemplateName( templateId ) );
		processInfo.setBeginTime( jpr.getCreateDate() );
		processInfo.setEndTime( jpr.getCompleteDate() );
		processInfo.setActive( jpr.isActive() );
		processInfo.setFinished( jpr.isFinished() );
		processInfo.setCanceled( jpr.isCanceled() );
		processInfo.setPaused( jpr.isPaused() );
		if (jpr.isPaused()) {
			processInfo.setState("Paused");
		}
		else {
			processInfo.setState( jpr.getState() );
		}
		JobProcessInfo.CompletionState cs = jpr.getCompletionState();
        processInfo.setCompletionState( cs != null? cs: SUCCESS );

		if (detailsScope != None) {
			GraphProcess process = engineFactory.findProcess( jpr.getId() );
			NodeFinder nodeFinder = new NodeFinder( process.getGraph() );
	        TaskChain tc = getTaskChainFactory().getTaskChain( templateId );
	        List<UserTaskInfoRecord> taskRecords = getTaskRecords( jpr.getId(), detailsScope );;
			List<TaskInfo> taskInfoList =
				getUserTasksInfoList( taskRecords, tc, nodeFinder );
			processInfo.setTaskInfoList( taskInfoList );

			// create new task counts record, not bound to session -
			// we will populate it anyway in populateUserTaskCounts() call,
			// and don't want hibernate to be confused with possibly changed values
			taskCounts = new ProcessTaskCountsRecord();
			populateUserTaskCounts( taskInfoList, taskCounts );
			taskCounts.setPercentCompleted( getPercentCompleted( taskRecords ) );
		}
		
        // set task state counts, if present
        if (taskCounts != null) {
        	int needsAttentionCount =
        			taskCounts.getTotalWaitCount() - taskCounts.getTimeWaitCount();
        	processInfo.setNeedsAttentionCount( needsAttentionCount );
        	processInfo.setTimeWaitingCount( taskCounts.getTimeWaitCount() );
        	processInfo.setActiveTasksCount( taskCounts.getActiveCount() );
        	Double percent = taskCounts.getPercentCompleted();
			processInfo.setPercentCompleted( percent != null? percent.doubleValue(): 0.0 );
        }
		
		if (jpr.isFinished() && !processInfo.isCanceled()) {
			processInfo.setPercentCompleted( 100.0 );
		}
		return processInfo;
	}
	
	List<UserTaskInfoRecord> getTaskRecords( long processId, DetailedInfoScope detailsScope )
	{
		List<UserTaskInfoRecord> list;
		
		switch (detailsScope) {
		case AllTasks:
			list = statusDao.getProcessTasks( processId );
			break;
		case ActiveTasks:
			list = statusDao.getActiveProcessTasks( processId );
			break;
		case LastTaskForItem:
			HashMap<String, UserTaskInfoRecord> latest = new HashMap<>();
			for (UserTaskInfoRecord tr: statusDao.getProcessTasks( processId )){
				// records are ordered by id, so just put them into map
				// - newer will replace older:
				latest.put( tr.getItemOrTask(), tr );
			}
			list = new ArrayList<>( latest.values() );
			Collections.sort( list, new Comparator<UserTaskInfoRecord>() {
						public int compare(UserTaskInfoRecord o1, UserTaskInfoRecord o2) {
							long diff = o1.getId() - o2.getId();
							return (diff > 0? 1: diff == 0? 0: -1);
						}
					});
			break;
		default:
			list = new ArrayList<>();
		}
	    return list;
	}
	
	
	List<TaskInfo> getUserTasksInfoList( List<UserTaskInfoRecord> utrList,
			                             TaskChain taskChain, 
			                             NodeFinder nodeFinder )
	{
		List<TaskInfo> taskInfoList = new ArrayList<TaskInfo>();
		
		for (UserTaskInfoRecord utr: utrList) {
			if (utr.getBeginTime() == null) { // did not start yet
				continue;
			}
			TaskSummary ts = taskChain.find( utr.getTaskName() );
			Task task = ts != null? ts.getTask():
				                    new Task( utr.getTaskName(), "", false);
			taskInfoList.add( taskInfoFromRecord( utr, task, nodeFinder ) );
		}
		return taskInfoList;
	}
	
	TaskInfo taskInfoFromRecord( UserTaskInfoRecord utr, 
			                     Task task, 
			                     NodeFinder nodeFinder )
	{
		TaskInfo taskInfo = new TaskInfo();
		taskInfo.setTask( task );
		taskInfo.setItem( utr.getItem() );
		taskInfo.setBeginTime( utr.getBeginTime() );
		taskInfo.setEndTime( utr.getEndTime() );
		taskInfo.setWaiting( utr.isWaiting() );
		taskInfo.setTimeWaiting( utr.isTimeWaiting() );
		// TODO: later 'needs attention' will be set only when workflow 
		// requires user input, for now it's just 'not time waiting':
		taskInfo.setNeedsAttention( taskInfo.isWaiting() && ! taskInfo.isTimeWaiting() );
		taskInfo.setWaitingMessage( utr.getWaitMessageWithoutPrefix() );
		taskInfo.setPaused( "paused".equalsIgnoreCase( utr.getCondition() ) );
		taskInfo.setCurrentState( utr.getCurrentState() );
		taskInfo.setErrorCategory( utr.getCondition() );
		taskInfo.setDetails( utr.getMessage() );
		
		taskInfo.setTokenId( utr.getLastTokenId() );
		taskInfo.setLastMainstreamAction( utr.getLastSuccessfulNodeName() );
		if (utr.isWaiting()) {
			// add resume actions if any
			Node node = nodeFinder.findById( utr.getLastNodeId() );
			if (node != null && node instanceof JobActionNode) {
				XmlAttributes attrs = ((JobActionNode)node).getAttributes();
				taskInfo.setResumeActions( EventManager.getResumeActions( attrs ) );
			}
		}
		return taskInfo;
	}
	
	void populateUserTaskCounts( List<TaskInfo> taskInfoList, 
			                     ProcessTaskCountsRecord taskCounts )
	{
		int needsAttentionCount = 0;
		int timeWaitingCount = 0;
		int activeCount = 0;
		
		for (TaskInfo taskInfo: taskInfoList) {
			if (taskInfo.getEndTime() == null) {
				activeCount++;
				if (taskInfo.isNeedsAttention()) {
					needsAttentionCount++;
				}
				if (taskInfo.isTimeWaiting()) {
					timeWaitingCount++;
				}
			}
		}
		
		taskCounts.setActiveCount( activeCount );
		taskCounts.setTimeWaitCount( timeWaitingCount );
		taskCounts.setTotalWaitCount( needsAttentionCount + timeWaitingCount );
	}
	
	double getPercentCompleted( List<UserTaskInfoRecord> taskRecords )
	{
		double sum = 0;
		int count = 0;
		for (UserTaskInfoRecord taskRecord: taskRecords) {
			if (taskRecord.getHead() != null) {
				Double percent = taskRecord.getPercentCompleted();
				sum += (percent != null? percent.doubleValue(): 0.0);
				count++;
			}
		}
		double percent = count > 0? sum/count: 0.0;
		return percent;
	}
	
	private Map<Integer, String> templNames = new LruCache<>( 20 );
	
	String getTemplateName( Integer templateId )
	{
		String name = templNames.get(  templateId );
		if (name == null) {
			
			JobTemplateRecord template = jobDao.findJobTemplate( templateId.longValue() );
			name = template.getName();
			templNames.put( templateId, name );
		}
		return name;
	}

	public TaskChainFactory getTaskChainFactory() {
		return taskChainFactory;
	}
	public void setTaskChainFactory(TaskChainFactory utcFactory) {
		this.taskChainFactory = utcFactory;
	}
	public void setStatusDao(JobStatusDao statusDao) {
		this.statusDao = statusDao;
	}
	public void setJobDao( JobDao jobDao ) {
		this.jobDao = jobDao;
	}
	public void setEngineFactory( EngineFactory engineFactory )	{
		this.engineFactory = engineFactory;
	}
}
