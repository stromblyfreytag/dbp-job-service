package com.trustwave.dbpjobservice.workflow;

import static com.googlecode.sarasvati.event.ExecutionEventType.PROCESS_PENDING_CANCEL;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.Duration;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Element;






import com.googlecode.sarasvati.Engine;
import com.googlecode.sarasvati.Graph;
import com.googlecode.sarasvati.GraphProcess;
import com.googlecode.sarasvati.Node;
import com.googlecode.sarasvati.NodeToken;
import com.googlecode.sarasvati.ProcessState;
import com.googlecode.sarasvati.env.Env;
import com.googlecode.sarasvati.load.GraphLoader;
import com.googlecode.sarasvati.xml.XmlProcessDefinition;
import com.trustwave.dbpjobservice.impl.ExtendedTemplateDescriptionCache;
import com.trustwave.dbpjobservice.impl.Messages;
import com.trustwave.dbpjobservice.workflow.api.util.XmlUtil;
import com.trustwave.dbpjobservice.xml.XmlExtendedDescription;
import com.trustwave.dbpjobservice.xml.XmlFlags;

public class WorkflowManager 
{
	private static Logger logger = LogManager.getLogger( WorkflowManager.class );
	private ExtendedTemplateDescriptionCache extendedTemplateDescriptionCache;
	private EngineFactory engineFactory;

	public void setExtendedTemplateDescriptionCache(
			ExtendedTemplateDescriptionCache templateDescriptionCache) {
		this.extendedTemplateDescriptionCache = templateDescriptionCache;
	}

	public EngineFactory getEngineFactory() {
		return engineFactory;
	}

	public void setEngineFactory(EngineFactory engineFactory) {
		this.engineFactory = engineFactory;
	}

	private void registerJobActionNode( Engine engine )
	{
		Class<? extends Node> clazz = JobActionNode.class;
	    engine.addNodeType( JobActionNode.JobActionNodeDiscriminator, clazz );
	    JaxbNodeFactory nf = 
	    	new JaxbNodeFactory( clazz, "com.appsec.jobservice.xml" );
	    engine.getFactory().addNodeFactory( JobActionNode.JobActionNodeDiscriminator, nf );
	}
	
	XmlProcessDefinition getProcessDefinition( Engine engine, String xmlStr )
	{
		registerJobActionNode( engine );
	    XmlContentLoader xmlLoader = new XmlContentLoader(); // "com.appsec.jobservice.xml" );
	    return xmlLoader.translateContent( xmlStr );
	}
	
	@Transactional
	public String getTemplateName( String xmlStr )
	{
	    Engine engine = createEngine();
	    XmlProcessDefinition def = getProcessDefinition( engine, xmlStr );
		return def.getName();
	}

	@Transactional
	public XmlExtendedDescription geExtendedTemplateDescription( String xmlStr )
	{
	    Engine engine = createEngine();
	    XmlProcessDefinition def = getProcessDefinition( engine, xmlStr );
	    List<Object> customList = def.getCustomProcessData();
	    if (customList.size() > 0) {
	    	Object custom = customList.get(0);
			if (custom instanceof Element) {
				String xmlBindingPackage = XmlExtendedDescription.class.getPackage().getName();
				custom = XmlUtil.fromDom( (Element)custom, xmlBindingPackage);
			}
			if (!(custom instanceof JAXBElement<?>)) {
				throw new RuntimeException( Messages.getString("workflow.customObject.unsupported", custom) );
			}
			@SuppressWarnings("unchecked")
			JAXBElement<XmlExtendedDescription> jaxbValue =
				(JAXBElement<XmlExtendedDescription>)custom;
			return jaxbValue.getValue();
	    }
		return null;
	}
	
	public XmlExtendedDescription geExtendedTemplateDescription( Graph graph )
	{
	    Long graphId = engineFactory.getGraphId( graph );
		return extendedTemplateDescriptionCache.getExtendedDescription( graphId );
	}
	
	public boolean isUtilityTemplate( Graph graph )
	{
		XmlFlags flags = geExtendedTemplateDescription( graph ).getFlags();
		if (flags != null) {
			return flags.isUtilityJob();
		}
		return false;
	}

	public Long getDeleteCompletedInstancesAfterMilliseconds( Graph graph )
	{
		XmlFlags flags = geExtendedTemplateDescription( graph ).getFlags();
		if (flags != null) {
			Duration duration = flags.getDeleteCompletedInstanceAfter();
			if (duration != null) {
				return duration.getTimeInMillis( Calendar.getInstance() );
			}
		}
		return null;
	}

	@Transactional
	public Graph importWorkflow( String xmlStr )
	{
	    Engine engine = createEngine();
	    XmlProcessDefinition def = getProcessDefinition( engine, xmlStr );
	    GraphLoader<? extends Graph> wfLoader = engine.getLoader();
	    wfLoader.loadDefinition( def );
	    Graph graph = engine.getRepository().getLatestGraph( def.getName() );
	    Long graphId = engineFactory.getGraphId( graph );
	    // remove old extended description from cache  
	    extendedTemplateDescriptionCache.purgeTemplate( graphId );
	    // pre-populate extendedTemplateDescriptionCache:
	    // (this is not required, but prevents warnings in WorkflowManager unit tests):
	    extendedTemplateDescriptionCache.getExtendedDescription( graphId, xmlStr );
		return graph;
	}

	@Transactional
	public Graph importWorkflowFile( String fileName )
	{
		return importWorkflow( readFile(fileName) );
	}
	
	public static String readFile( String fileName )
	{
		StringBuffer buf = new StringBuffer();
		FileReader reader = null;
		try {
			File file = new File( fileName );
			reader = new FileReader(file);
			char[] cbuf = new char[4096];
			int len = reader.read( cbuf );
			while (len > 0) {
				buf.append( cbuf, 0, len );
				len = reader.read( cbuf );
			}
		} 
		catch (Exception e) {
			throw new RuntimeException( e );
		}
		finally {
			if (reader != null) {
				try { reader.close(); } catch (IOException e) {}
			}
		}
		return buf.toString();
	}


	public Graph getLatestGraph( String name )
	{
	    Engine engine = createEngine();
	    Graph graph = engine.getRepository().getLatestGraph(name);
		return graph;
	}

	@Transactional
	public List<String> getAllGraphNames() 
	{
	    Engine engine = createEngine();
	    List<? extends Graph> graphs = engine.getRepository().getGraphs();
	    Set<String> names = new HashSet<String>();
	    for (Graph g: graphs) {
	    	if (isUtilityTemplate( g )) {
	    		continue;
	    	}
	    	names.add( "" + g.getName() );
	    }
		return new ArrayList<String>( names );
	}
	
	public NodeToken getTokenById( long tokenId, long processId )
	{
	    return engineFactory.getTokenById( tokenId, processId );
	}
	
	@Transactional
	public GraphProcess loadProcess( long processId )
	{
	    return findProcess( processId );
	}
	
	@Transactional
	public void executeWorkflowTask( WorkflowTask wftask )
	{
		long time0 = System.currentTimeMillis();
	    Engine engine = createEngine( wftask.getProcessId() );
	    executeWorkflowTask0( wftask, engine );
		if (logger.isDebugEnabled()) {
			long time = System.currentTimeMillis() - time0;
			logger.debug( "Executed " + wftask + ", time=" + time );
		}
	}
	
	@Transactional
	public void executeWorkflowTasks( List<WorkflowTask> wftasks, Holder<WorkflowTask> lastTaskHolder )
	{
		int batchSize = wftasks.size();
		long time0 = System.currentTimeMillis();
		if (logger.isDebugEnabled()) {
			logger.debug( "Executing " + batchSize + " workflow tasks: " + wftasks );
		}
		

	    while (wftasks.size() > 0) {
	    	WorkflowTask wftask = wftasks.remove(0);
	    	lastTaskHolder.set(wftask);
		    Engine engine = createEngine( wftask.getProcessId() );
	    	executeWorkflowTask0( wftask, engine );
	    }
	    
		if (logger.isDebugEnabled() && batchSize > 1) {
			long time = System.currentTimeMillis() - time0;
			logger.debug( "Execution of " + batchSize + " tasks took " + time + "ms." );
		}
	}
	
	private void executeWorkflowTask0( WorkflowTask wftask, Engine engine  )
	{
		//MDC.put( "processId", String.valueOf( wftask.getProcessId() ) );
		wftask.setWfManager( this );
		wftask.setEngine( engine );
		wftask.run();
		//MDC.put( "processId", "" );
	}
	
	public void startCancelProcess(long processId, Engine engine) 
	{
		logger.debug( "Begin canceling process " + processId );
		GraphProcess process = findProcess( processId );
		engine.addExecutionListener(process, CancelProcessListener.class, PROCESS_PENDING_CANCEL);
		engine.cancelProcess(process);
	}
	
	public void finalizeCancelProcess(long processId, Engine engine) 
	{
		logger.debug( "Finalize canceling process " + processId );
		GraphProcess process = findProcess( processId );
		engine.finalizeCancel(process);
	}
	
	public void completeToken( NodeToken token,
                               String       arcName, 
			                   Engine    engine )
	{
	    engine.completeAsynchronous( token, arcName );
	}
	
	public void completeWithTokenSet( NodeToken        token, 
                                      String              arcName, 
			                          String              tokenSetName,
			                          Env                 tokenSetEnv,
			                          Map<String,List<?>> tokenSetMemberEnv, 
			                          Engine           engine )
	{
		if (tokenSetMemberEnv == null || tokenSetMemberEnv.size() == 0) {
			throw new RuntimeException( "Empty token set" );
		}
	    List<?> sample = tokenSetMemberEnv.values().iterator().next();
	    int size = sample.size();
	    logger.debug( "completeWithTokenSet '" + tokenSetName + "', size=" + size
	    		    + ", keys=" + tokenSetMemberEnv.keySet() );
	    engine.completeWithNewTokenSet( token,         // token to complete
	    		                        arcName,
	    		                        tokenSetName,
	    		                        size,              // token set size
	    		                        true,              // asynchronous
	    		                        tokenSetEnv,       // initial env
	    		                        tokenSetMemberEnv  // per-thread init env
	    		                      );
	}
	
	public void continueProcess( long processId, Engine engine ) 
	{
	    GraphProcess process = findProcess( processId );
	    
	    if (process == null) {
	    	logger.error( "Cannot find process " + processId );
	    	return;
	    }
	    
	    if (process.getState() == ProcessState.Created) {
	    	logger.debug( "Starting process " + processId );
	    	engine.startProcess( process );
	    }
	    else {
	    	engine.executeQueuedArcTokens( process );
	    }
	}

	public void resumeProcess( long processId, Engine engine ) 
	{
	    GraphProcess process = findProcess( processId );

	    // register with process-completion  listener
		ProcessCompletedListener.registerProcess( process, engine );
		
		Collection<NodeToken> tokens = process.getActiveNodeTokens();
		if (tokens.size() > 0) {
			// re-execute not completed nodes
			for (NodeToken token: tokens) {
				token.getNode().execute( engine, token );
			}
		}
		else {
			// no active tokens, just push it:
			continueProcess( processId, engine );
		}
	}
	
	private Engine createEngine()
	{
		return engineFactory.createEngine();
	}
	
	public Engine createEngine( long processId )
	{
		return engineFactory.createEngineForProcess( processId );
	}
	
	public GraphProcess findProcess( long processId )
	{
		return engineFactory.findProcess( processId );
	}
	
}
