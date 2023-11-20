package com.trustwave.dbpjobservice.workflow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.googlecode.sarasvati.Arc;
import com.googlecode.sarasvati.Graph;
import com.googlecode.sarasvati.Node;

public class GraphLinealizer 
{
	private Graph graph;
	private boolean defaultArcsOnly;
	private List<Node> linearized = null;
	private HashSet<Node> processedNodes = new HashSet<Node>();
	private HashSet<Node> newBeginNodes;
	
	public GraphLinealizer( Graph graph ) 
	{
		this( graph, false );
	}

	public GraphLinealizer( Graph graph, boolean defaultArcsOnly ) 
	{
		this.graph = graph;
		this.defaultArcsOnly = defaultArcsOnly;
	}

	public List<Node> getLinearized()
	{
		if (linearized == null) {
			linearized = new ArrayList<Node>();
			linearize();
		}
		return linearized;
	}
	
	private void linearize()
	{
		HashSet<Node> beginNodes = new HashSet<Node>();
		beginNodes.addAll( graph.getStartNodes() );
		while (!beginNodes.isEmpty()) {
			newBeginNodes = new HashSet<Node>();
			for (Node node: beginNodes) {
				linearize( node, true, null );
			}
			beginNodes = newBeginNodes;
		}
	}

	private void linearize( Node node, boolean firstNodeInTheChain, String incomingArcName ) 
	{
		if (processedNodes.contains( node ) ) {
			return;
		}
		if (incomingArcName != null) {
			// Actions achievable only through non-default (named) arcs
			// should not be included into linearized action list
			// (i.e. ignored in the job state/progress calculations),
			// unless they have 'mainstream' attribute set to 'true':
			Node n = WorkflowUtils.resolveNodeReference( node );
			if (n instanceof JobActionNode) {
				if (!((JobActionNode)n).getAttributes().getActionType().isMainstream()) {
					return;
				}
			}
		}
		// if the node is the first node in the chain, we should add it to
		// the linearized list without further checks;
		if (!firstNodeInTheChain) {
			// otherwise, if the node is a merge node, we should end
			// current chain and put the merge node into the list of
			// begin nodes - to be processed in the next cycle. 
			if (WorkflowUtils.isMergeNode(node)) {
				newBeginNodes.add( node );
				return;
			}
		}
		linearized.add( node );
		processedNodes.add( node );
		
		for (Arc arc: graph.getOutputArcs( node )) {
			if (arc.getName() != null && defaultArcsOnly) {
				continue;
			}
			linearize( arc.getEndNode(), false, arc.getName() );
		}
	}
}
