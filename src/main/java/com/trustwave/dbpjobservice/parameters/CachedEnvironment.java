package com.trustwave.dbpjobservice.parameters;

import java.util.HashMap;


import com.googlecode.sarasvati.GraphProcess;
import com.googlecode.sarasvati.NodeToken;
import com.googlecode.sarasvati.TokenSet;
import com.googlecode.sarasvati.env.Env;
import com.trustwave.dbpjobservice.impl.Messages;

public class CachedEnvironment extends TypedEnvironment 
{
	public enum Type { TokenSet, Process }; 
	
	private HashMap<String, Object> cache = new HashMap<String, Object>();
	private Type type;
	private String name;
	private NodeToken token = null;
	private GraphProcess process = null;
	
	public CachedEnvironment( String name, Type type, Env env ) 
	{
		super( env, null );
		this.name = name;
		this.type = type;
	}
	
	public CachedEnvironment( String name, Type type, NodeToken token, CachedEnvironment upperEnv ) 
	{
		super( null, upperEnv ); // see updateEnv() comment
		this.token = token;  
		this.name = name;
		this.type = type;
	}
	
	@Override
	public boolean hasAttribute( String name )
	{
		return getAttribute( name ) != null;
	}
	
	@Override
	public synchronized Object getAttribute( String name )
	{
		Object value;
		if (cache.containsKey(name)) {
			value = cache.get( name );
		}
		else {
			value = super.getAttribute( name );
			cache.put( name, value );
		}
		return value;
	}
	

	public synchronized void setAttribute( String name, Object value, boolean Transient )
	{
		super.setAttribute( name, value, Transient );
		cache.put( name, value );
	}
	
	/**
	 * Update environment from current token to avoid accessing stale
	 * persistent environment
	 * @param token
	 */
	synchronized void updateEnv( NodeToken token )
	{
		// Well, we will do lazy update - to avoid re-reading env from database
		// e.g. for completely cached environment.
		// Instead of storing corresponding token environment, we will store
		// token itself - to retrieve environment from it when/if it is needed.
		// Non-null token is also a sign for getEnv() method to refresh env.
		this.token = token;
		
		if (getHigherLevelEnv() != null) {
			getHigherLevelEnv().updateEnv( token );
		}
	}
	
	synchronized void updateEnv( GraphProcess process )
	{
		// Same as for token above, but for process. Also, no higher level env
		if (type !=  Type.Process) {
			throw new RuntimeException(Messages.getString("workflow.error.envType.wrong"));
		}
		this.process = process;
	}
	
	@Override
	protected synchronized Env getEnv()
	{
		if (process != null) {
			// re-read process env:
			setEnv( process.getEnv() );
			process = null;
		}
		if (token != null) {
			// re-read env from token:
			if (type == Type.Process) {
				setEnv( token.getProcess().getEnv() );
			}
			else if (type == Type.TokenSet) {
				TokenSet ts = token.getTokenSet( name );
				if (ts == null) {
					throw new RuntimeException(Messages.getString("workflow.error.noTokenSet", name));
				}
				// tokenset environment may be kept either in the TokenSet object
				// (new behavior) or in the token itself (old behavior)
				if (ts.getEnv().getAttributeNames().iterator().hasNext()) {
					setEnv( ts.getEnv() );
				}
				else {
					// TokenSet.env is empty, old behavior - use token env:
					setEnv( token.getEnv() );
				}
			}
			token = null;
		}
		return super.getEnv();
	}
}
