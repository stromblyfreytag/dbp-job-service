package com.trustwave.dbpjobservice.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.trustwave.dbpjobservice.workflow.WorkflowManager;
import com.trustwave.dbpjobservice.xml.XmlExtendedDescription;

public class ExtendedTemplateDescriptionCache
{
	private static Logger logger = LogManager.getLogger( ExtendedTemplateDescriptionCache.class );
	
	private WorkflowManager workflowManager;
	private JobManager      jobManager;
	private Map<Long, XmlExtendedDescription> cache =
			new HashMap<Long, XmlExtendedDescription>();
	
	public void init()
	{
		// Code below resolves circular dependency:
		// jobManager -> workflowManager -> extendedTemplateDescriptionCache 
		//                               -> (workflowManager,jobManager).
		// Due to this dependency Spring cannot populate property
		// workflowManager.extendedTemplateDescriptionCache.
		// There is no easy way to get rid of this dependency, so we resolve it here:
		workflowManager.setExtendedTemplateDescriptionCache( this );
	}

	public XmlExtendedDescription getExtendedDescription( long templateId )
	{
		return getExtendedDescription( templateId, null );
	}
	
	public synchronized void purgeTemplate( long templateId )
	{
		cache.remove( templateId );
	}
	
	public synchronized XmlExtendedDescription getExtendedDescription( long templateId, String xml )
	{
		XmlExtendedDescription description = cache.get( templateId );
		if (description == null) {
			description = retrieveExtendedDescription( templateId, xml );
			cache.put( templateId, description );
		}
		return description;
	}
	
	private XmlExtendedDescription retrieveExtendedDescription( long templateId, String xml )
	{
		logger.debug( "Retrieving extended description for templateId=" + templateId );
		XmlExtendedDescription dsc = null;
		try {
			if (xml == null) {
				xml = jobManager.getTemplateXml(templateId);
			}
			dsc = workflowManager.geExtendedTemplateDescription( xml );
		} 
		catch (Exception e) {
			logger.warn( "Error retrieving extended description for templateId="
		               + templateId + ": " + e + "; ignored" );
		}
		return (dsc != null? dsc: new XmlExtendedDescription());
	}

	public void setWorkflowManager(WorkflowManager workflowManager) {
		this.workflowManager = workflowManager;
	}
	public void setJobManager(JobManager jobManager) {
		this.jobManager = jobManager;
	}
}
