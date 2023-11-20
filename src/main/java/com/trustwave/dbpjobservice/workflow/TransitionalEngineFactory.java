package com.trustwave.dbpjobservice.workflow;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import com.googlecode.sarasvati.Engine;
import com.googlecode.sarasvati.Graph;
import com.googlecode.sarasvati.GraphProcess;
import com.googlecode.sarasvati.NodeToken;
import com.trustwave.dbpjobservice.impl.Messages;

public class TransitionalEngineFactory extends EngineFactory
{
	private static Logger logger = LogManager.getLogger( TransitionalEngineFactory.class );
	
	private EngineFactory newFactory;
	private EngineFactory oldFactory;
	private long maxOldProcessId = Long.MIN_VALUE;
	
	private long getMaxOldProcessId()
	{
		if (maxOldProcessId == Long.MIN_VALUE) {
			maxOldProcessId = oldFactory.getLastProcessId();
			logger.info( "MaxOldProcessId=" + maxOldProcessId );
		}
		return maxOldProcessId;
	}

	@Override
	public Engine createEngine()
	{
		return newFactory.createEngine();
	}

	@Override
	public Engine createEngineForProcess( long processId )
	{
		if (processId <= getMaxOldProcessId()) {
			return oldFactory.createEngineForProcess( processId );
		}
		return newFactory.createEngineForProcess( processId );
	}
	
	@Override
	public GraphProcess findProcess( long processId )
	{
		if (processId <= getMaxOldProcessId()) {
			return oldFactory.findProcess( processId );
		}
		return newFactory.findProcess( processId );
	}

	@Override
	protected GraphProcess newProcess( Graph graph, Engine engine )
	{
		return newFactory.newProcess( graph, engine );
	}

	@Override
	public void deleteProcess( long processId )
	{
		if (processId <= getMaxOldProcessId()) {
			oldFactory.deleteProcess( processId );
		}
		else {
			newFactory.deleteProcess( processId );
		}
	}

	@Override
	public NodeToken getTokenById( long tokenId, long processId )
	{
		if (processId <= getMaxOldProcessId()) {
			return oldFactory.getTokenById( tokenId, processId ); 
		}
		return newFactory.getTokenById( tokenId, processId ); 
	}

	@Override
	public Long getProcessId( GraphProcess process )
	{
		if (newFactory.isOwnProcessClass( process.getClass() )) {
			return newFactory.getProcessId( process );
		}
		if (oldFactory.isOwnProcessClass( process.getClass() )) {
			return oldFactory.getProcessId( process );
		}
		throw new RuntimeException( Messages.getString("workflow.process.class.unrecognized", process.getClass()) );
	}

	@Override
	public Long getGraphId( Graph graph )
	{
		return newFactory.getGraphId( graph );
	}

	public void setNewFactory( EngineFactory currentfactory ) {
		this.newFactory = currentfactory;
	}

	public void setOldFactory( EngineFactory oldFactory ) {
		this.oldFactory = oldFactory;
	}
}
