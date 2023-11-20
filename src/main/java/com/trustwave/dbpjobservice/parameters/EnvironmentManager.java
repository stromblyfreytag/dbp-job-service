package com.trustwave.dbpjobservice.parameters;

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;






import com.googlecode.sarasvati.GraphProcess;
import com.googlecode.sarasvati.NodeToken;
import com.googlecode.sarasvati.NodeTokenSetMember;
import com.googlecode.sarasvati.TokenSet;
import com.googlecode.sarasvati.env.Env;
import com.googlecode.sarasvati.impl.MapEnv;
import com.trustwave.dbpjobservice.impl.Messages;
import com.trustwave.dbpjobservice.parameters.CachedEnvironment.Type;
import com.trustwave.dbpjobservice.workflow.EngineFactory;
import com.trustwave.dbpjobservice.workflow.WorkflowUtils;
import com.trustwave.dbpjobservice.workflow.api.action.JobAction;

public class EnvironmentManager 
{
	private static Logger logger = LogManager.getLogger( EnvironmentManager.class );
	
	public static final String TOKEN_SET_ATTR = "tokenSet";
	private static final String PROCES_KEY = "\b--process--\b";
	
	private EngineFactory engineFactory;
	
	private HashMap<Long, HashMap<String, CachedEnvironment>> procEnvCache =
			new HashMap<Long, HashMap<String,CachedEnvironment>>();
	
	public CachedEnvironment getProcessEnvironment( GraphProcess process )
	{
		Long processId = getProcessId( process );
		return getProceessEnv( process, processId );
	}
	
	public TypedEnvironment getTokenEnvironment( NodeToken token )
	{
		Long processId = getProcessId( token.getProcess() );
		Env env = null;
		CachedEnvironment higherLevelEnv = null; 
		
		String tsName = token.getEnv().getAttribute( TOKEN_SET_ATTR );
		if (tsName == null) {
			// ordinary token
			env = token.getEnv();
			higherLevelEnv = getProceessEnv( token.getProcess(), processId );
		}
		else {
			// tokenset token
			NodeTokenSetMember sm = token.getTokenSetMember( tsName );
			if (sm == null) {
				throw new RuntimeException(Messages.getString("workflow.error.token.tokenSet", token));
			}
			env = sm.getEnv();
			higherLevelEnv = findOrCreateEnv( processId, tsName, token );
		}
		return new TypedEnvironment( env, higherLevelEnv );
	}
	
	public void removeProcessEnvironments(Long processId )
	{
		procEnvCache.remove( processId );
	}
	
	private Long getProcessId( GraphProcess process ) 
	{
		return engineFactory.getProcessId( process );
	}

	private synchronized CachedEnvironment getProceessEnv( GraphProcess process, Long processId )
	{
		HashMap<String,CachedEnvironment> procEnvMap =
				getProcessEnvironments( processId );
		CachedEnvironment procEnv = procEnvMap.get( PROCES_KEY );
		if (procEnv == null) {
			procEnv = new CachedEnvironment( PROCES_KEY, Type.Process, process.getEnv() );
			procEnvMap.put( PROCES_KEY, procEnv );
		}
		else {
			procEnv.updateEnv( process );
		}
		return procEnv;
	}
		
	private synchronized CachedEnvironment findOrCreateEnv( Long processId, String tsName, NodeToken token )
	{
		HashMap<String,CachedEnvironment> procEnvMap =
				getProcessEnvironments( processId );
		CachedEnvironment tsEnv = procEnvMap.get( tsName );
		if (tsEnv == null) {
			CachedEnvironment higherLevelEnv = 
					getProceessEnv( token.getProcess(), processId );
			tsEnv = new CachedEnvironment( tsName, Type.TokenSet, token, higherLevelEnv );
			procEnvMap.put( tsName, tsEnv );
		}
		else {
			tsEnv.updateEnv( token );
		}
		return tsEnv;
	}
	
	private HashMap<String,CachedEnvironment> getProcessEnvironments( Long processId )
	{
		HashMap<String,CachedEnvironment> procEnvMap = procEnvCache.get( processId );
		if (procEnvMap == null) {
			procEnvMap = new HashMap<String, CachedEnvironment>();
			procEnvCache.put( processId, procEnvMap );
		}
		return procEnvMap;
	}
	
	public Env createTokenSetEnvironment( NodeToken token, String tsName )
	{
	    MapEnv env = new MapEnv();
	    env.importEnv( token.getEnv() );
	    for (String name: env.getAttributeNames() ) {
    		token.getEnv().removeAttribute( name );
	    }
	    // status message should not be passed to tokenset - or it will override
	    // null (absent) status message from tokenset actions.
		env.removeAttribute( JobAction.PARAMETER_STATUS_MESSAGE );
		token.getEnv().setAttribute( TOKEN_SET_ATTR, tsName );
		if (logger.isDebugEnabled()) {
			logger.debug( "createTokenSet(" + tsName + "): env="
		                + WorkflowUtils.asString( token.getEnv() )
					    + "; ts=" + WorkflowUtils.asString( env ) );
		}
		return env;
	}
	
	public void closeTokenSetEnvironment( NodeToken token )
	{
		String tsName = null;
		TokenSet ts = WorkflowUtils.getTokenSet( token, TOKEN_SET_ATTR );
		if (ts != null) {
			token.getEnv().importEnv( ts.getEnv() );
			tsName = ts.getName();
		}
		token.getEnv().removeAttribute( TOKEN_SET_ATTR );
		
		if (logger.isDebugEnabled()) {
			logger.debug( "after closeTokenSet(" + tsName + "): "
					    + (ts == null? "ts=null, ": "")
					    + "env=" + WorkflowUtils.asString( token.getEnv() ) );
		}
	}
	
	public String getTokenSetName( NodeToken token )
	{
		return token.getEnv().getAttribute( TOKEN_SET_ATTR );
	}

	public void setEngineFactory( EngineFactory engineFactory )
	{
		this.engineFactory = engineFactory;
	}
}
