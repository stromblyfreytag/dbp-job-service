package com.trustwave.dbpjobservice.workflow;

import java.util.ArrayList;
import java.util.List;

import com.trustwave.dbpjobservice.interfaces.Event;
import com.trustwave.dbpjobservice.interfaces.ResumeAction;
import com.trustwave.dbpjobservice.workflow.api.action.IJobAction;
import com.trustwave.dbpjobservice.xml.XmlAttributes;
import com.trustwave.dbpjobservice.xml.XmlEventDescriptor;

public class EventManager
{
	private ActionManager actionManager;
	
	public static ResumeAction getResumeAction( XmlEventDescriptor eventDsc )
	{
		ResumeAction action = new ResumeAction();
		action.setButtonLabel( eventDsc.getButton() );
		action.setEventName(   eventDsc.getName() );
		action.setDescription( eventDsc.getDescription() );
		return action;
	}
	
	public static List<ResumeAction> getResumeActions( XmlAttributes attrs )
	{
		List<ResumeAction> actions = new ArrayList<ResumeAction>();
		for (XmlEventDescriptor eventDsc: attrs.getOnEvent()) {
			if (eventDsc.getButton() != null) {
				actions.add( getResumeAction( eventDsc ) );
			}
		}
		return actions;
	}

	public void onEvent( Event event )
	{
		List<IJobAction> actions = actionManager.getActionsWaitingForEvent( event.getName() );
		for (IJobAction action: actions) {
			if (actionMatchesEvent( action, event )) {
				actionManager.eventReceived( action, getEventDescriptor( action, event ) );
			}
		}
		
	}

	public static boolean hasEventDescriptor( XmlAttributes attrs, String eventName )
	{
		for (XmlEventDescriptor event: attrs.getOnEvent()) {
			if (event.getName().equals( eventName )) {
				return true;
			}
		}
		return false;
	}

	public static boolean actionMatchesEvent( IJobAction action, Event event ) 
	{
		if (event.getProcessId() != null && action.getProcessId() != event.getProcessId()) {
			return false;
		}
		if (event.getItem() != null && !event.getItem().equals( action.getItem() )) {
			return false;
		}
		return true;
	}
	
	public static XmlEventDescriptor getEventDescriptor( IJobAction action, Event event )
	{
		for (XmlEventDescriptor eventDsc: action.getNodeAttributes().getOnEvent()) {
			if (eventDsc.getName().equals( event.getName() )) {
				return eventDsc;
			}
		}
		return null;
	}

	public ActionManager getActionManager() {
		return actionManager;
	}
	public void setActionManager(ActionManager actionManager) {
		this.actionManager = actionManager;
	}
}
