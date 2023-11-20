package com.trustwave.dbpjobservice.parameters;



import static com.trustwave.dbpjobservice.workflow.api.action.IJobAction.PARAMETER_ERROR_DETAILS;
import static com.trustwave.dbpjobservice.workflow.api.action.IJobAction.PARAMETER_EXIT_CONDITION;
import static com.trustwave.dbpjobservice.workflow.api.action.IJobAction.PARAMETER_FAILED_ACTION;
import static com.trustwave.dbpjobservice.workflow.api.action.IJobAction.PARAMETER_ITEM;
import static com.trustwave.dbpjobservice.workflow.api.action.IJobAction.PARAMETER_STATE;
import static com.trustwave.dbpjobservice.workflow.api.action.IJobAction.PARAMETER_TASK_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


















import com.googlecode.sarasvati.GraphProcess;
import com.googlecode.sarasvati.Node;
import com.googlecode.sarasvati.NodeToken;
import com.trustwave.dbpjobservice.actions.RequiredParameterValidator;
import com.trustwave.dbpjobservice.actions.ValidationDescriptor;
import com.trustwave.dbpjobservice.impl.Messages;
import com.trustwave.dbpjobservice.impl.exceptions.WorkflowException;
import com.trustwave.dbpjobservice.interfaces.ExternalParameterDefinition;
import com.trustwave.dbpjobservice.interfaces.ExternalParameterValue;
import com.trustwave.dbpjobservice.workflow.JobActionNode;
import com.trustwave.dbpjobservice.workflow.WorkflowUtils;
import com.trustwave.dbpjobservice.workflow.api.action.IJobAction;
import com.trustwave.dbpjobservice.workflow.api.action.InputParameter;
import com.trustwave.dbpjobservice.workflow.api.action.JobAction;
import com.trustwave.dbpjobservice.workflow.api.action.JobContext;
import com.trustwave.dbpjobservice.workflow.api.action.OutputParameter;
import com.trustwave.dbpjobservice.xml.XmlOutputEnv;
import com.trustwave.dbpjobservice.xml.XmlParameter;
import com.trustwave.dbpjobservice.xml.XmlParameters;
import com.trustwave.dbpjobservice.xml.XmlValidator;

public class ParameterManager 
{
	private static Logger logger = LogManager.getLogger( ParameterManager.class );
	
	private EnvironmentManager environmentManager;
	private Map<Long, List<ParameterDescriptor>> nodeActionDescriptors =
		new HashMap<Long, List<ParameterDescriptor>>();
	private Map<String, ParameterDescriptor> freeParameters =
		new HashMap<String, ParameterDescriptor>();
	private Map<String, ParameterDescriptor> environmentVariables =
			new HashMap<String, ParameterDescriptor>();
	private XmlParameters declaredParameters;
	private List<ParameterDescriptor> orderedFreeParameters =
			new ArrayList<ParameterDescriptor>();
	
	
	public ParameterManager() {
	}

	public ParameterManager( XmlParameters declaredParameters, 
			                 List<Node> nodes,
			                 EnvironmentManager environmentManager,
			                 boolean validateJobParameters )
	{
		this.declaredParameters = declaredParameters;
		this.environmentManager = environmentManager;
		gatherParameters( nodes, validateJobParameters );
	}
	
	public ParameterManager( XmlParameters declaredParameters,
			                 EnvironmentManager environmentManager,
			                 Node ... nodes )
	{
		this( declaredParameters, Arrays.asList( nodes ), environmentManager, true );
	}
	
	public void gatherParameters( List<Node> workflowNodes, 
			                      boolean validateJobParameters )
	{
		for (Node n: workflowNodes) {
			if (n instanceof JobActionNode) {
				addParameterDescriptors( (JobActionNode)n );
			}
		}
		
		if (declaredParameters != null) {
			orderedFreeParameters =	
					ParameterUtils.orderParametersByDeclaredList(
							freeParameters.values(), declaredParameters,
							validateJobParameters );
		}
		else {
			orderedFreeParameters = 
					new ArrayList<ParameterDescriptor>( freeParameters.values() );
		}
	}
	
	private void addParameterDescriptors( JobActionNode node )
	{
		Class<?> actionClass = node.getActionClass();
		List<ParameterDescriptor> list =
			ParameterUtils.retrieveParameterDescriptors( actionClass );
		checkMissingDescriptors( node, list );
		
		for (ParameterDescriptor pd: list) {
			ParameterUtils.completeWithXmlParameter( pd,
					                      node.getXmlParameter( pd.getName() ),
					                      node.getXmlOutput( pd.getName() ) );
			if (pd.isFreeParameter()) {
				logger.debug( "Adding to free parameters: " + pd.getInputName()
						    + ", node=" + node.getName() );
				XmlParameter declared = findDeclaredParameter( pd.getInputName() );
				if (declared != null) {
					ParameterUtils.completeWithXmlParameter( pd, declared, null );
				}
				freeParameters.put( pd.getInputName(), pd );
			}
		}
		checkNodeParameters( node, list );
		
		ParameterUtils.appendFreeOutputDescriptors(
				list, node.getAttributes().getOutput(), actionClass, node.getName() );
		
		nodeActionDescriptors.put( node.getId(), list );

		// gather parameter descriptors declaring output into environment:
		for (ParameterDescriptor pd: list) {
			if (pd.isOutputToProcessEnvironment()) {
				String name = pd.getExternalName();
				ParameterDescriptor pd0 = environmentVariables.get( name );
				if (pd0 != null) {
					if (!pd0.isExternallyCompatibleWith( pd )) {
						throw new ParameterException(Messages.getString("param.envVar.definition.incompatible", name));
					}
				}
				else {
					environmentVariables.put( name, pd );
				}
			}
		}
	}
	
	private XmlParameter findDeclaredParameter( String name )
	{
		if (declaredParameters != null) {
			for (XmlParameter p: declaredParameters.getParameter()) {
				if (name.equals( p.getName() )) {
					return p;
				}
			}
		}
		return null;
	}
	
	static void checkMissingDescriptors( JobActionNode node, List<ParameterDescriptor> pdlist )
	{
		Set<String> pdNames = new HashSet<String>();
		for (ParameterDescriptor pd: pdlist) {
			pdNames.add( pd.getName() );
		}
		
		Class<?> actionClass = node.getActionClass();
		List<String> annotatedAsParameters = 
				BeanUtils.getAnnotatedFields( actionClass, 
						InputParameter.class, OutputParameter.class );
		
		for (String fieldName: annotatedAsParameters) {
			if (!pdNames.contains( fieldName)) {
				throw new RuntimeException( Messages.getString("param.getterSetter.notFound", fieldName, actionClass.getName()) );
			}
		}
	}
	
	
	/** <p>Check that all parameters mentioned in the node xml map to 
	 * the corresponding action parameters.</p>
	 * <p>Throws exception if xml parameter does not have corresponding action
	 *  parameter.</p>
	 * @param node node object to check
	 * @param pdlist list of declared input parameters discovered in the node action.
	 */
	void checkNodeParameters( JobActionNode node, List<ParameterDescriptor> pdlist )
	{
		List<XmlParameter> params = node.getAttributes().getParameter();
		if (params == null) {
			return;
		}
		
		Map<String, ParameterDescriptor> pdMap =
			new HashMap<String, ParameterDescriptor>();
		for (ParameterDescriptor pd: pdlist) {
			pdMap.put( pd.getName(), pd );
		}
		for (XmlParameter p: params) {
			String name = p.getInternalName();
			if (name == null)
				name = p.getName();
			if (!pdMap.containsKey( name )) {
				throw new ParameterException( Messages.getString("param.unknown", name, node.getName()) );
			}
		}
	}
	
	public Map<String, ParameterDescriptor> getFreeParameters() 
	{
		return freeParameters;
	}
	
	public List<ParameterDescriptor> getOrderedFreeParameters()
	{
		return orderedFreeParameters;
	}
	
	public ParameterDescriptor getFreeParameter( String parameterName )
	{
		return freeParameters.get( parameterName );
	}

	public ParameterDescriptor getFreeParameterNotNull( String parameterName )
	{
		ParameterDescriptor pd = getFreeParameter( parameterName );
		if (pd == null) {
			throw new ParameterException(Messages.getString("param.descriptor.notFound", parameterName));
		}
		return pd;
	}

	public Map<String, ParameterDescriptor> getEnvironmentVariables() 
	{
		return environmentVariables;
	}
	
	public ParameterDescriptor getEnvironmentVariable( String name )
	{
		return environmentVariables.get( name );
	}
	
	public List<ParameterDescriptor> getParameterDescriptorsForNode( long nodeId )
	{
		List<ParameterDescriptor> list = nodeActionDescriptors.get( nodeId );
		return (list != null? list: new ArrayList<ParameterDescriptor>());
	}
	
	public boolean hasJoinParameters( IJobAction action )
	{
		List<ParameterDescriptor> descriptors = 
			getParameterDescriptorsForNode( action.getNodeId() );
		for (ParameterDescriptor pd: descriptors) {
			if (pd.getJoinType() != null) {
				return true;
			}
		}
		return false;
	}
	
	public Map<String,Object> getActionParameters( IJobAction jaction, NodeToken token ) 
	{
		JobAction action = jaction.getOriginalAction();
		Map<String,Object> map = new HashMap<String, Object>();
		List<ParameterDescriptor> descriptors =
			getParameterDescriptorsForNode( action.getNodeId() );

		for (ParameterDescriptor pd: descriptors) {
			Object value = null;
			try {
				if (pd.getGetter() != null) {
					value =  pd.getGetter().invoke( action );
				}
				else if (token != null) {
					value =  token.getEnv().getAttribute( pd.getName() );
				}
			} 
			catch (Exception e) {
				value = e;
			}

			map.put( pd.getName(), value );
		}
		return map;
	}
	
	public TypedEnvironment getEnvironment( NodeToken token )
	{
		return environmentManager.getTokenEnvironment( token );
	}
	
	public Object getParameterValue( String parameterName, NodeToken token,
			                         TypedEnvironment tenv )
	{
		ParameterDescriptor pd = 
			findParameter( parameterName, token.getNode().getId() );
		if (pd == null) {
			return null;
		}
		return getParameterValue( pd, token, tenv, true );
	}
	
	private ParameterDescriptor findParameter( String name, long nodeId )
	{
		for (ParameterDescriptor pd: getParameterDescriptorsForNode(nodeId) ) {
			if (pd.getName().equals( name ))
				return pd;
		}
		return null;
	}
	
	public void populateActionParameters( IJobAction action, NodeToken token )
	{
		populateActionParameters( action, token, false );
	}
	
	public void populateActionParameters( IJobAction jaction, NodeToken token, boolean safe,
			                            String ... parameter )
	
	{
		JobAction action = jaction.getOriginalAction();
		List<String> pnames = Arrays.asList( parameter );
		if (logger.isDebugEnabled())
			logger.debug( "Populating " + action + " with parameters " + pnames );

		TypedEnvironment tenv = getEnvironment( token );
		Set<String> genericParams = new HashSet<String>();
		List<ParameterDescriptor> descriptors = 
			getParameterDescriptorsForNode( action.getNodeId() );
		
		for (ParameterDescriptor pd: descriptors) {
			if (pnames.size() > 0 && !pnames.contains( pd.getName())) {
				continue;
			}
			if (pd.getGenericName() != null) {
				genericParams.add( pd.getGenericName() );
			}
			if (action.isParameterPopulated( pd.getName())) {
				continue;
			}
			Object value = null;
			if (safe) {
				try {
					value = getParameterValue( pd, token, tenv, safe );
				}
				catch (Exception e) {}
			}
			else { 
				value = getParameterValue( pd, token, tenv, safe );
			}
			
			if (pd.getSetter() == null) {
				if (value != null && logger.isDebugEnabled()) {
					logger.debug( "No setter for " + pd.getName()
							   + " parameter in "  + action
							   + " value ignored: '" + value + "'" );
				}
				continue;
			}
			setActionParameter( pd, value, action, safe );
		}
		
		// now check if all generic parameters have been set:
		if (!safe) {
			for (String pname: genericParams) {
				if (!action.isParameterValuePresent( pname )) {
					throw new ParameterException( "No value for parameter " + pname + ", " + action );
				}
			}
		}
	}
	
	private Object getParameterValue( ParameterDescriptor pd, 
			                          NodeToken token,
			                          TypedEnvironment tenv, 
			                          boolean safe ) 
	{
		Object value = null;

		if (pd.getJoinType() != null) {
			List<NodeToken> parentTokens = WorkflowUtils.getParentTokens(token);
			if ("map".equals(pd.getJoinType())) {
				value = populateJoinMap(pd, parentTokens);
			}
			else {
				value = populateJoinList(pd, parentTokens);
			}
		}
		else {
			value = tenv.getAttribute( pd.getInputName() );
			
			if (pd.isInputParameter() && pd.getValueExpression() != null
			 && pd.isOverrideTokenValue()) {
				ExpressionEvaluator evaluator = new ExpressionEvaluator();
				evaluator.setIgnoreExpressionErrors(
						safe | pd.isIgnoreExpressionErrors() );
				value = evaluator.evaluate(
							pd.getValueExpression(), tenv, pd.getValueType());
			}
			if (value == null && pd.getDefaultValue() != null) {
				ExpressionEvaluator evaluator = new ExpressionEvaluator();
				evaluator.setIgnoreExpressionErrors(
						safe | pd.isIgnoreExpressionErrors() );
				value = evaluator.evaluate(
							pd.getDefaultValue(), tenv, pd.getValueType());
			}
		}
		return value;
	}

	private Map<Object,Object> populateJoinMap( ParameterDescriptor pd,
			                                    List<NodeToken>     parentTokens )
	{
		Map<Object,Object> map = new HashMap<Object,Object>();
		String discriminatorAttr = pd.getJoinDiscriminator();
		for (NodeToken token: parentTokens) {
			TypedEnvironment tenv = getEnvironment( token );
			Object discriminator = tenv.getAttribute( discriminatorAttr );
			if (discriminator == null) {
				logger.error( "no value for discriminator '" + discriminatorAttr + "', token=" + token.getId() );
				continue;
			}
			Object value = tenv.getAttribute( pd.getInputName() );
			map.put( discriminator, value );
		}
		return map;
	}
	
	private List<Object> populateJoinList( ParameterDescriptor pd,
			                               List<NodeToken>     parentTokens ) 
	{
		List<Object> list = new ArrayList<Object>();
		for (NodeToken token : parentTokens) {
			TypedEnvironment tenv = getEnvironment( token );
			Object value = tenv.getAttribute( pd.getInputName() );
			list.add( value);
		}
		return list;
	}

	public String getTokenSetName( IJobAction action )
	{
		List<ParameterDescriptor> descriptors = 
			getParameterDescriptorsForNode( action.getNodeId() );
		
		for (ParameterDescriptor pd: descriptors) {
			if (pd.getTokenSet() != null)
				return pd.getTokenSet(); 
		}
		return null;
	}
	
	public Map<String,List<?>> createTokenSetMemberEnv( IJobAction jaction, NodeToken token )
	{
		JobAction action = jaction.getOriginalAction();
		TypedEnvironment tenv = getEnvironment( token );
		Map<String,List<?>> map = new HashMap<String, List<?>>();
		
		List<ParameterDescriptor> descriptors = 
			getParameterDescriptorsForNode( action.getNodeId() );
		
		String tsName = null;
		int    size   = 0;
		
		for (ParameterDescriptor pd: descriptors) {
			if (pd.getTokenSet() != null) {
				tsName = (tsName == null? pd.getTokenSet(): tsName);
				if (!tsName.equals( pd.getTokenSet())) {
					throw new WorkflowException(Messages.getString("workflow.tokenSet.different", action));
				}
				if (pd.getElementValueType() == null) {
					throw new WorkflowException(Messages.getString("workflow.elementType.unknown", pd, action));
				}
				List<?> list;
				try {
					list = (List<?>)pd.getGetter().invoke( action );
				}
				catch (Exception e) {
					throw new WorkflowException( Messages.getString("workflow.prop.list.canNotRetrieve", pd.getName(), action, e.toString()), e );
				}
				logger.debug( "tokenset '" + tsName + "' member: "
				            + pd.getName() + ", size=" + (list != null? list.size(): "<null>") );
				if (list != null && list.size() > 0) {
					size = (size == 0? list.size(): size);
					if (list.size() != size) {
						throw new WorkflowException(Messages.getString("workflow.set.size.different", action));
					}
					map.put( pd.getOutputName(), list );
					// Sarasvati will populate tokenset tokens with attribute values only,
					// without type, so save attribute type now:
					tenv.saveAttributeType( pd.getOutputName(),
							                pd.getElementValueType() );
				}
			}
		}
		return map;
	}
	
	private void setActionParameter( ParameterDescriptor pd, 
			                       Object value,
			                       IJobAction action,
			                       boolean safe )
	{
		if (logger.isTraceEnabled())
			logger.trace( "Setting " + pd.getName() + " to '" + value + "'"
				        + (pd.getInputName().equals(pd.getName())? ""
				    		            : " from " + pd.getInputName()) );
		
		action.markParameterPopulated( pd.getName(), value != null );
		if (pd.getGenericName() != null) {
			if (!action.isParameterValuePresent( pd.getGenericName() )) {
				action.markParameterPopulated( pd.getGenericName(), value != null );
			}
		}
		if (value == null) {
			if (!pd.isInputParameter() || pd.isOptional()) {
				// do nothing 
				return;
			}
			else if (pd.getGenericName() != null) {
				// generic parameter - ignore null value:
				return;
			}
			else {
				String msg = Messages.getString("workflow.param.noValue", pd.getName(), action);
				if (safe) {
					logger.warn( msg );
				}
				else {
					throw new WorkflowException( msg  );
				}
			}
		}
		try {
			pd.getSetter().invoke( action, value );
		} 
		catch (Exception e) {
			throw new WorkflowException( Messages.getString("workflow.param.value.canNotSet", pd, action, e) );
		} 
	}
	
	public void saveActionOutput( IJobAction jaction, NodeToken token, boolean actionComplete )
	{
		JobAction action = jaction.getOriginalAction();
		jaction.releaseLockInMemory();

		if (logger.isDebugEnabled())
			logger.debug( "Saving action output " + action);
		
		List<ParameterDescriptor> descriptors = 
				getOutputParameterDescriptorsForNode( action.getNodeId() );
		TypedEnvironment tenv = getEnvironment( token );
		
		for (ParameterDescriptor pd: descriptors) {
			Object value = parameterValueToSave( pd, tenv, action, actionComplete );
			
			// decide which environment to use for output:
			TypedEnvironment outenv = tenv;
			if (XmlOutputEnv.PROCESS.equals( pd.getOutputEnv() )) {
				outenv = environmentManager.getProcessEnvironment( token.getProcess() );
			}
			
			if (logger.isTraceEnabled())
				logger.trace( "Saving " + pd.getName() + "='" + value + "'"
				            + (pd.getOutputName().equals(pd.getName())?
				        		    "": " as " + pd.getOutputName()) ); 
			
			outenv.setAttribute( pd.getOutputName(), value, pd.isTransient() );
		}
	}
	
	// returns list of output parameter descriptors for the specified node.
	// Parameters that have output value with expression are last in the list
	List<ParameterDescriptor> getOutputParameterDescriptorsForNode( long nodeId )
	{
		List<ParameterDescriptor> descriptors =
				getParameterDescriptorsForNode( nodeId );
		List<ParameterDescriptor> listWithoutExpr = new ArrayList<>();
		List<ParameterDescriptor> listWithExpr = new ArrayList<>();
		
		for (ParameterDescriptor pd: descriptors) {
			if (!pd.isOutputParameter())   // only output parameters
				continue;
			if (pd.getTokenSet() != null)  // token set parameters are set with tokeSetEnvMap
				continue;
			String outval = pd.getOutputValueExpression();
			if (outval != null && (outval.contains("${") || outval.contains("#{") )) {
				listWithExpr.add( pd );
			}
			else {
				listWithoutExpr.add( pd );
			}
		}
		listWithoutExpr.addAll( listWithExpr );
		return listWithoutExpr;
	}
	
	
	private Object parameterValueToSave( ParameterDescriptor pd,
			TypedEnvironment tenv, IJobAction action, boolean allowUsingOutputValue )
	{
		if (pd.isClearOutputValue()) {
			if (logger.isDebugEnabled())
				logger.debug( "Clearing value of " + pd + " in " + action );
			return null;
		}
		
		Object value = null;
		if (pd.getGetter() != null) {  // xml-defined parameters don't have getter  
			try {
				value =  pd.getGetter().invoke( action );
			} 
			catch (Exception e) {
				throw new WorkflowException( Messages.getString("workflow.param.value.canNotGet", pd.getName(), action, e.toString()), e );
			}
		}
		if (value == null || pd.isOverrideActionOutputValue()) {
			String expr = pd.getOutputValueExpression();
			if (expr != null && allowUsingOutputValue) {
				if (logger.isDebugEnabled())
					logger.debug( "Using expression value for " + pd.getName() + ": " + expr + ", " + action );
				ExpressionEvaluator evaluator = new ExpressionEvaluator();
				evaluator.setIgnoreExpressionErrors( pd.isIgnoreExpressionErrors() );
				value = evaluator.evaluate( expr, tenv, pd.getValueType() );
			}
		}
		return value;
	}

	/**
	 * Save only well-known state parameters - exception-safe method
	 */
	public void saveStateOnly( IJobAction action, NodeToken token )
	{
		TypedEnvironment tenv = getEnvironment( token );
		tenv.setAttribute( PARAMETER_STATE,          action.getState() );
		tenv.setAttribute( PARAMETER_EXIT_CONDITION, action.getExitCondition() );
		tenv.setAttribute( PARAMETER_ERROR_DETAILS,  action.getErrorDetails() );
		tenv.setAttribute( PARAMETER_FAILED_ACTION,  action.getFailedAction() );
		tenv.setAttribute( PARAMETER_TASK_NAME,      action.getTaskName() );
		tenv.setAttribute( PARAMETER_ITEM,           action.getItem() );
	}

	public List<ValidationDescriptor> prepareValidationList( List<ExternalParameterValue> params )
	{
		List<ValidationDescriptor> validationList =
				new ArrayList<ValidationDescriptor>();
		Map<String, Object> paramValues = new HashMap<String, Object>();
		
		for (ExternalParameterValue p: params) {
			ParameterDescriptor pd = getFreeParameterNotNull( p.getName() );
			Object value = pd.getExternalizer().externalValueToObject( p );
			// accumulate for for required parameter validators below: 
			paramValues.put( p.getName(), value );
			if (value != null) {
				for (XmlValidator xv: pd.getValidators()) {
					ValidationDescriptor vd =
							new ValidationDescriptor( xv, p.getName(), value, null );
					validationList.add( vd );
				}
			}
		}
		
		addRequiredParametersValidators( paramValues, validationList );
		logger.debug( "Validation list: " + validationList );
		
		return validationList;
	}
	
	void addRequiredParametersValidators( Map<String,Object> paramValues,
			                              List<ValidationDescriptor> validationList )
	{
		for (ParameterDescriptor pd: freeParameters.values()) {
			if (!pd.isOptional()) {
				String pname = pd.getInputName();
				Object value = paramValues.get( pd.getInputName() );
				ValidationDescriptor vr =
						new ValidationDescriptor(
								RequiredParameterValidator.createXmlValidator(),
								pname, value, pd.getGenericName() );
				validationList.add( vr );
			}
		}
	}
	
	
	public void validateParameters( List<ExternalParameterValue> params, 
			                        JobContext context,
			                        List<String> errors )
	{
		List<ValidationDescriptor> validationList =
				prepareValidationList( params );
		
		for (ValidationDescriptor vd: validationList) {
			vd.validate( context );
		}
		ValidationDescriptor.cleanRequiredErrorsForGenericParams( validationList );
		
		for (ValidationDescriptor vd: validationList) {
			if (vd.hasErrors()) {
				errors.addAll( vd.getFormattedErrors() );
			}
		}
	}
	
	public void populateProcessEnv( GraphProcess process, Map<String,String> paramValues )
	{
		TypedEnvironment tenv = environmentManager.getProcessEnvironment( process );
		for (String pname: paramValues.keySet()) {
			ParameterDescriptor pd = getFreeParameter( pname );
			if (pd != null) {
				Object value = pd.stringToObject( paramValues.get(pname) );
				tenv.setAttribute( pname, value, false );
			}
		}
	}
	
	public List<ExternalParameterDefinition> createDefinitions()
	{
		List<ExternalParameterDefinition> defs = new ArrayList<ExternalParameterDefinition>();
		
		for (ParameterDescriptor pd: getOrderedFreeParameters()) {
			if (!pd.isVisible()) {
				continue;
			}
			ExternalParameterDefinition def =
				pd.getExternalizer().createExternalDefinition();
			defs.add( def );
		}
		return defs;
	}
	
}
