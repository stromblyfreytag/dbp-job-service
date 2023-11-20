package com.trustwave.dbpjobservice.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class WorkflowLoader
{
	private static Logger logger = LogManager.getLogger(WorkflowLoader.class);
	private static final String WORKFLOWS_DIR = "workflows";

	private JobManager jobManager;
	private boolean loading = false;
	private List<String> errors = new ArrayList<String>();
	private Map<String, String>  workflowFiles = new HashMap<String, String>();
	private File workflowsDir = null;

	public void setJobManager(JobManager jobManager)
	{
		this.jobManager = jobManager;
	}
	
	private synchronized boolean testAndSetLoading()
	{
		boolean loading0 = loading;
		loading = true;
		return loading0;
	}
	
	private synchronized void doneLoading()
	{
		loading = false;
	}

	public List<String> loadWorkflows()
	{
		if (testAndSetLoading()) {
			logger.debug( "Loading is in progress" );
			return getErrors();
		}
		errors.clear();
		workflowFiles.clear();
		jobManager.resetPropertySubstitutor();
		loadWorkflows0();
		return getErrors();
	}
	
	private void loadWorkflows0()
	{
		try {
			File directory = getWorkflowsDir();
			if (!directory.exists()) {
				String msg = Messages.getString("workflow.dir.doesNotExist", directory.getAbsolutePath());
				errors.add( msg );
				return;
			}
			logger.info("Loading workflows from " + directory.getAbsolutePath());
			
			for (File file : directory.listFiles()) {
				if (file.getName().toLowerCase().endsWith(".xml")) {
					InputStream is = null;
					try {
						is = new FileInputStream(file);
						String workflowXml = IOUtils.toString(is);
						checkWorkflowOverriding( file.getName(), workflowXml );
						jobManager.createTemplate(workflowXml);
					}
					catch (Exception e) {
						String msg = Messages.getString("workflow.template.canNotLoad", file.getName(), e.getMessage());
						logger.error( msg );
						errors.add( msg );
					}
					finally {
						if (is != null) {
							try { is.close(); } catch (Exception e) {}
						}
					}
				}
			}
			logger.debug( "Loading completed" );
		}
		catch (Exception e) {
			logger.error( e.toString(), e );
			errors.add( "ERROR: " + e.toString() );
		}
		finally {
			doneLoading();
		}
	}
	
	private void checkWorkflowOverriding( String fileName, String workflowXml )
	{
		String name = jobManager.getWorkflowManager().getTemplateName( workflowXml );
		if (workflowFiles.containsKey(name)) {
			String prevFile = workflowFiles.get( name ); 
			String msg = Messages.getString("workflow.file.override", name, fileName, prevFile);
			logger.warn( msg );
			errors.add( msg );
		}
		workflowFiles.put( name, fileName );
	}

	public List<String> getErrors() 
	{
		return new ArrayList<String>( errors );
	}

	private File getWorkflowsDir()
	{
		if (workflowsDir == null) {
			String installDir = System.getProperty("com.appsec.basedir");
			workflowsDir = new File(installDir + "\\" + WORKFLOWS_DIR);
		}
		return workflowsDir; 
	}

	// for unit tests only!
	void setWorkflowsDir(File workflowsDir) {
		this.workflowsDir = workflowsDir;
	}
	
	
}
