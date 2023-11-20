package com.trustwave.dbpjobservice.workflow;

import com.googlecode.sarasvati.event.EventActionType;
import com.googlecode.sarasvati.event.EventActions;
import com.googlecode.sarasvati.event.ExecutionEvent;
import com.googlecode.sarasvati.event.ExecutionListener;

public class CancelProcessListener implements ExecutionListener {

	@Override
	public EventActions notify(ExecutionEvent event) 
	{
		return new EventActions(EventActionType.DELAY_PROCESS_FINALIZE_CANCEL);
	}
}
