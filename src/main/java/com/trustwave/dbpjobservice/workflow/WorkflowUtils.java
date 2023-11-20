package com.trustwave.dbpjobservice.workflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.googlecode.sarasvati.Arc;
import com.googlecode.sarasvati.ArcToken;
import com.googlecode.sarasvati.Graph;
import com.googlecode.sarasvati.JoinType;
import com.googlecode.sarasvati.Node;
import com.googlecode.sarasvati.NodeToken;
import com.googlecode.sarasvati.TokenSet;
import com.googlecode.sarasvati.env.Env;
import com.googlecode.sarasvati.hib.HibNode;
import com.googlecode.sarasvati.hib.HibNodeRef;

public class WorkflowUtils 
{
	/**
	 * Returns all parent tokens of the given token
	 * @param token
	 * @return
	 */
	public static List<NodeToken> getParentTokens( NodeToken token )
	{
		List<NodeToken> ptokens = new ArrayList<NodeToken>();
		for (ArcToken arct: token.getParentTokens()) {
			ptokens.add( arct.getParentToken() );
		}
		return ptokens;
	}
	
	/**
	 * Return list of nodes that will be executed after given node
	 * with the specified output arc name.
	 * @param node
	 * @param arcName
	 * @return
	 */
	public static List<Node> getNextNodes( Node node, String arcName )
	{
		List<Node> nodes = new ArrayList<Node>();
		List<Arc> arcs = node.getGraph().getOutputArcs( node, arcName );
		for (Arc a: arcs) {
			nodes.add( a.getEndNode() );
		}
		return nodes;
	}
	
	/**
	 * Converts list of nodes that make contain node references 
	 * to the list of exact nodes
	 * @param nodes
	 * @return
	 */
	public static List<Node> resolveNodeReferences( List<Node> nodes )
	{
		List<Node> resolved = new ArrayList<Node>();
		for (Node n: nodes) {
			if (n instanceof HibNodeRef) {
				n = ((HibNodeRef)n).getNode();
			}
			resolved.add( n );
		}
		return resolved;
	}
	
	public static Node resolveNodeReference( Node node )
	{
		if (node != null) {
			if (node instanceof HibNodeRef)
				node = ((HibNodeRef)node).getNode();
		}
		return node; 
	}
	
	public static List<Node> getGraphNodes( Graph graph )
	{
		return resolveNodeReferences( graph.getNodes() );
	}
	
	public static List<Node> linearizeGraph( Graph graph )
	{
		GraphLinealizer gl = new GraphLinealizer( graph );
		List<Node> nodes = gl.getLinearized();
		return resolveNodeReferences( nodes );
	}
	
	/**
	 * Returns <code>true</code> if the given node is a <i>merge node</i>,
	 * i.e. the one on which process will wait until all tokens
	 * on incoming arcs are completed. 
	 * @param node
	 * @return <code>true</code> if the given node is a <i>merge node</i>,
	 *         <code>false</code> otherwise.  
	 */
	public static boolean isMergeNode( Node node )
	{
		// node with tokenSet join is ALWAYS a merge node. 
		if (isTokenSetMergeNode( node ))
			return true;
		
		Collection<Arc> inArcs = node.getGraph().getInputArcs( node );
		if (inArcs.size() > 1) {
			// more than 1 incoming arcs.
			// For simplicity assume that any waiting join type
			// (i.e. anything but OR-join) makes this node a merge node:
			return (!JoinType.OR.equals( node.getJoinType() ));
		}
		return false;
	}

	public static boolean isTokenSetMergeNode( Node node )
	{
		if (JoinType.TOKEN_SET.equals(node.getJoinType()))
			return true;
		if (JoinType.TOKEN_SET_OR.equals(node.getJoinType()))
			return true;
		return false;
	}
	
	/**
	 * Retrieves current or just completed (joined) token set,
	 * or null if there is no token set.
	 * @param token Token from which to retrieve token set
	 * @param tsAttrName name of the attribute for token set name
	 */
	public static TokenSet getTokenSet( NodeToken token, String tsAttrName )
	{
		String tsName = token.getEnv().getAttribute( tsAttrName );
		TokenSet ts = (tsName != null? token.getTokenSet( tsName ): null);
		if (ts == null && isTokenSetMergeNode( token.getNode() )) {
			// if this is a merge node token,
			// token set should be present in a parent token:
			List<ArcToken> arcs = token.getParentTokens();
			if (arcs.size() > 0) {
				NodeToken parent = arcs.get(0).getParentToken();
				if (tsName == null) {
					tsName = parent.getEnv().getAttribute( tsAttrName );;
				}
				ts = parent.getTokenSet( tsName );
			}
		}
		return ts;
	}
	
	public static String asString( Env env )
	{
		return envAsMap( env, true ).toString(); 
	}
	
	private static final int MAX_CHARACTERS_IN_ATTR_VALUE = 40;
	
	public static Map<String, String> envAsMap( Env env, boolean shortStr )
	{
		Map<String, String> map = new HashMap<String, String>();
		for (String name: env.getAttributeNames()) {
			String value = env.getAttribute( name );
			if (shortStr && value != null && value.length() > MAX_CHARACTERS_IN_ATTR_VALUE) {
				value = value.substring(0, MAX_CHARACTERS_IN_ATTR_VALUE) + "...";
			}
			map.put( name, value );
		}
		return map;
	}
	
	public static Long getNodeId( Node node )
	{
		if (node instanceof HibNode) {
			return ((HibNode)node).getId();
		}
		if (node instanceof HibNodeRef) {
			return ((HibNodeRef)node).getNode().getId();
		}
		return null;
	}
		
}
