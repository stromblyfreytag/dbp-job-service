package com.trustwave.dbpjobservice.workflow;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;







import com.googlecode.sarasvati.Arc;
import com.googlecode.sarasvati.Graph;
import com.googlecode.sarasvati.Node;
import com.googlecode.sarasvati.impl.MapEnv;
import com.trustwave.dbpjobservice.impl.Messages;
import com.trustwave.dbpjobservice.parameters.ExpressionEvaluator;
import com.trustwave.dbpjobservice.parameters.TypedEnvironment;
import com.trustwave.dbpjobservice.workflow.api.action.IJobAction;
import com.trustwave.dbpjobservice.xml.XmlExitCondition;
import com.trustwave.dbpjobservice.xml.XmlSelectArc;

/**
 * This class is responsible for processing and verification of &lt;selectArc&gt;
 * elements (choosing next arc/node based on current action exit condition
 * and environment) 
 * @author vlad
 *
 */
public class ArcEvaluator 
{
	private static Logger logger = LogManager.getLogger( ArcEvaluator.class );
	private IJobAction action;
	private TypedEnvironment tenv;
	private ExpressionEvaluator evaluator;
	
	public ArcEvaluator(IJobAction action, TypedEnvironment tenv)
	{
		this.action = action;
		this.tenv = tenv;
	}

	public String evaluateArc()
	{
		evaluator = new ExpressionEvaluator();
		String arc;
		try {
			arc = tryEvaluateArc();
		}
		catch (Exception e) {
			logger.error( "Error evaluating arc for " + action
					    + ": "  + e.getMessage() );
			
			// force error condition and try to re-select arc ignoring errors:
			logger.warn( "Setting exit condition for  " + action
					   + " from " + action.getExitCondition() 
					   + " to " + XmlExitCondition.WORKFLOW_ERROR );
			action.setCondition( XmlExitCondition.WORKFLOW_ERROR, e.getMessage() );
			evaluator.setIgnoreExpressionErrors( true );
			try {
				arc = tryEvaluateArc();
			}
			catch (Exception e1) {
				logger.error( "Error re-evaluating arc for " + action, e );
				// use default arc as the last resort - though most probably it's wrong
				// TODO: maybe we should not catch and let caller terminate process instead. 
				arc = null;
			}
		}
		
		logger.debug( "Arc[" + action + "] =  "  + (arc != null? arc: "(default)") );
		return arc;
	}
	
	private String tryEvaluateArc()
	{
		String arc = selectArc();
		if (arc != null) {
			arc = evaluator.evaluate( arc, tenv, String.class );
			if ("".equals(arc))
				arc = null;
		}
		return arc;
	}
	
	// If action received external event and event descriptor (<onEvent>)
	// specifies arc, return this arc.
	//
	// Otherwise loop through <selectArc> elements and choose the first one
	// for which ALL conditions (if, unless, when) evaluate to 'true'.
	// Return arc specified in this element.
	private String selectArc()
	{
		if (action.getEventReceived() != null) {
			String arc = action.getEventReceived().getArc();
			if (arc != null && arc.length() > 0) {
				return arc;
			}
		}
		XmlExitCondition exitCond = action.getExitCondition();
		if (exitCond == null) {
			exitCond = XmlExitCondition.OK;
		}
		List<XmlSelectArc> selectList = action.getNodeAttributes().getSelectArc();
		
		for (XmlSelectArc sa: selectList) {
			if (sa.getIf() != null && exitCond != sa.getIf()) {
				continue;
			}
			if (sa.getUnless() != null && exitCond == sa.getUnless()) {
				continue;
			}
			if (sa.getWhen() != null) {
				boolean ok = evaluator.evaluateCondition( sa.getWhen(), tenv );
				if (!ok) {
					continue;
				}
			}
			// return the first arc for which all conditions are satisfied
			return arc(sa.getArc());
		}
		return null;
	}
	
	public static void checkArcConditions( Node node, Graph graph )
	{
		ExpressionEvaluator evaluator = new ExpressionEvaluator();
		evaluator.setIgnoreExpressionErrors( true );
		TypedEnvironment env = new TypedEnvironment( new MapEnv() );
		
		List<XmlSelectArc> condList;
		Node resolved = WorkflowUtils.resolveNodeReference( node );
		if (resolved instanceof JobActionNode) {
			condList = ((JobActionNode)resolved).getAttributes().getSelectArc();
		}
		else {
			condList = new ArrayList<>();
		}
		
		for (XmlSelectArc cond: condList) {
			
			if (graph != null) {
				List<Arc> arcs = graph.getOutputArcs( node, arc(cond.getArc()) );
				if (arcs.size() == 0) {
					throw new RuntimeException( Messages.getString("workflow.node.arc.notFound", node.getName(), cond.getArc()) );
				}
			}
			if (cond.getWhen() != null) {
				try {
					evaluator.evaluateCondition( cond.getWhen(), env );
				} 
				catch (Exception e) {
					throw new RuntimeException(	Messages.getString("workflow.node.selectArc.error", node.getName(), e.getMessage()) );
				}
			}
		}
	}

	public static void checkArcConditions( List<Node> nodes, Graph graph, boolean failIfValidationFails )
	{
		for (Node node: nodes) {
			try {
				checkArcConditions( node, graph );
			}
			catch (RuntimeException e) {
				if (failIfValidationFails) {
					throw e;
				}
				logger.warn( "Ignoring arc validation failure: " + e.getMessage() );
			}
		}
	}

	// returns null (default arc) for empty arc name
	private static String arc( String arcName )
	{
		return (arcName == null || arcName.isEmpty()? null: arcName);
	}
}
