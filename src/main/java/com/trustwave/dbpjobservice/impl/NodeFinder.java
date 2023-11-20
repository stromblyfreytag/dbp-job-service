package com.trustwave.dbpjobservice.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.googlecode.sarasvati.Graph;
import com.googlecode.sarasvati.Node;
import com.trustwave.dbpjobservice.workflow.WorkflowUtils;

public class NodeFinder 
{
	private Graph graph;
	private Map<Long,Node> cache = new HashMap<Long, Node>();

	public NodeFinder(Graph graph)
	{
		this.graph = graph;
		List<Node> nodes = WorkflowUtils.getGraphNodes( graph );
		
		for (Node node: nodes) {
			Long nodeId = WorkflowUtils.getNodeId( node );
			if (nodeId != null) {
				cache.put( nodeId, node );
			}
		}
	}
	
	public Node findById( Long id )
	{
		return cache.get( id );
	}

	public Graph getGraph() {
		return graph;
	}
}
