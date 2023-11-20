package com.trustwave.dbpjobservice.workflow;

import static com.googlecode.sarasvati.event.ExecutionEventType.PROCESS_CANCELED;
import static com.googlecode.sarasvati.event.ExecutionEventType.PROCESS_COMPLETED;
import static com.googlecode.sarasvati.event.ExecutionEventType.PROCESS_PENDING_CANCEL;
import static com.googlecode.sarasvati.event.ExecutionEventType.PROCESS_PENDING_COMPLETE;
import static com.googlecode.sarasvati.event.ExecutionEventType.PROCESS_STARTED;

import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;




import com.googlecode.sarasvati.Engine;
import com.googlecode.sarasvati.GraphProcess;
import com.googlecode.sarasvati.event.EventActions;
import com.googlecode.sarasvati.event.ExecutionEvent;
import com.googlecode.sarasvati.event.ExecutionEventType;
import com.googlecode.sarasvati.event.ExecutionListener;
import com.trustwave.dbpjobservice.backend.JobProcessRecord;
import com.trustwave.dbpjobservice.impl.JobProcessManager;
import com.trustwave.dbpjobservice.impl.JobStatusManager;

/**
 * Listener for Sarasvati process completes/canceled events
 */
public class ProcessCompletedListener implements ExecutionListener 
{
	private static Logger logger = LogManager.getLogger( ProcessCompletedListener.class );

	public static void registerProcess( GraphProcess process, Engine engine )
	{
		engine.addExecutionListener(
				process, ProcessCompletedListener.class,
				PROCESS_STARTED, 
				PROCESS_PENDING_COMPLETE, PROCESS_COMPLETED,
				PROCESS_PENDING_CANCEL, PROCESS_CANCELED );
	}
	
	@Override
	public EventActions notify( ExecutionEvent event ) 
	{
		ExecutionEventType eventType = event.getEventType();
		logger.debug( "Got " + eventType + " event for " + event.getProcess() );
		
		GraphProcess proc = event.getProcess();
		Long processId = getEngineFactory().getProcessId( proc );

		try {
			JobProcessRecord jpr =
				getProcessmanager().getProcessRecordById( processId );
			jpr.updateStatus( proc.getState() );
			if (PROCESS_COMPLETED == eventType || PROCESS_CANCELED == eventType) {
				onProcessCompleted( proc, jpr, event );
			}
		} 
		catch (Exception e) {
			logger.error( "Error updating status for processId=" +  processId
					    + ": " + e.getMessage(), e );
		}
		return null;
	}
	
	private void onProcessCompleted( GraphProcess process, 
			                         JobProcessRecord jpr,
			                         ExecutionEvent event )
	{
		jpr.setCompleteDate( new Date() );
		jpr.setActive( false );
		JobStatusManager.getInstance().onProcessCompleted( jpr );
		JobProcessManager.getInstance().onProcessCompleted( jpr, event.getEventType() );
	}
	
	private JobProcessManager getProcessmanager()
	{
		return JobProcessManager.getInstance();
	}
	
	private EngineFactory getEngineFactory()
	{
		return getProcessmanager().getEngineFactory();
	}
}
