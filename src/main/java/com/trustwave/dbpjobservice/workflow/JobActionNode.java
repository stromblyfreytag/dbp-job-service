package com.trustwave.dbpjobservice.workflow;

import static com.trustwave.dbpjobservice.workflow.ActionExecutor.Command.*;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;










import com.googlecode.sarasvati.Engine;
import com.googlecode.sarasvati.NodeToken;
import com.googlecode.sarasvati.annotations.NodeType;
import com.googlecode.sarasvati.hib.HibPropertyNode;
import com.trustwave.dbpjobservice.impl.JobProcessManager;
import com.trustwave.dbpjobservice.impl.Messages;
import com.trustwave.dbpjobservice.impl.exceptions.WorkflowException;
import com.trustwave.dbpjobservice.workflow.api.action.IJobAction;
import com.trustwave.dbpjobservice.workflow.api.action.JobAction;
import com.trustwave.dbpjobservice.workflow.api.util.XmlUtil;
import com.trustwave.dbpjobservice.xml.ObjectFactory;
import com.trustwave.dbpjobservice.xml.XmlAttributes;
import com.trustwave.dbpjobservice.xml.XmlOutput;
import com.trustwave.dbpjobservice.xml.XmlParameter;

@Entity
@DiscriminatorValue( JobActionNode.JobActionNodeDiscriminator )
@NodeType( "Generic wrapper for DbProtect job action nodes" )
public class JobActionNode extends HibPropertyNode 
{
	private static Logger logger = LogManager.getLogger( JobActionNode.class );
	
	public static final String JobActionNodeDiscriminator = "job-action";
	
	@Transient
	private XmlAttributes attributes;
	@Transient
	private Class<? extends JobAction> actionClass;

	public JobActionNode() {
	}
	
	// used for testing only
	public JobActionNode( String attrXml, Long id ) 
	{
		setProperty( "attrString", attrXml );
		setId( id );
	}
	
	public XmlAttributes getAttributes() {
		if (attributes == null) {
			this.attributes =
				XmlUtil.fromXmlString( getAttributesString(), XmlAttributes.class );
		}
		return attributes;
	}

	// this method is called by JaxbNodeFactory.assignToBean()
	public void setAttributes( XmlAttributes attributes ) {
		this.attributes = attributes;
		setProperty( "attrString", 
			XmlUtil.toXmlString( new ObjectFactory().createAttributes( attributes ) ) );
	}
	
	public String getAttributesString() {
		return getProperty("attrString");
	}

	@SuppressWarnings("unchecked")
	public <T> T getAdaptor( Class<T> clazz )
	{
		if (XmlAttributes.class.equals(clazz)) {
			return (T)getAttributes();
		}
		return super.getAdaptor( clazz );
	}
	
	@SuppressWarnings("unchecked")
	@Transient
	public Class<? extends JobAction> getActionClass()
	{
		if (actionClass == null) {
			String className = getAttributes().getActionType().getClazz();
			try {
				actionClass = (Class<? extends JobAction>)Class.forName( className );
			}
			catch (ClassNotFoundException e) {
				throw new WorkflowException( Messages.getString("workflow.action.class.invalid", className, getName(), e.toString()) );
			}
			if (!JobAction.class.isAssignableFrom( actionClass)) {
				throw new WorkflowException( Messages.getString("workflow.action.class.wrong", className, getName()) );
			}
		}
		return actionClass;
	}
	
	@Transient
	public XmlParameter getXmlParameter( String name )
	{
		for (XmlParameter p: getAttributes().getParameter()) {
			String xname = p.getInternalName();
			if (xname == null)
				xname = p.getName(); 
			if (name.equals( xname )) {
				return p;
			}
		}
		if (IJobAction.PARAMETER_TASK_NAME.equals( name )) {
			if (getAttributes().getTask() != null) {
				XmlParameter p = new XmlParameter();
				p.setName( name );
				p.setValue( getAttributes().getTask().getName() );
				return p;
			}
		}
		return null;
	}
	
	@Transient
	public XmlParameter getTaskNameParameter()
	{
		return getXmlParameter( IJobAction.PARAMETER_TASK_NAME );
	}
	
	@Transient
	public XmlOutput getXmlOutput( String name )
	{
		for (XmlOutput p: getAttributes().getOutput()) {
			String xname = p.getInternalName();
			if (xname == null)
				xname = p.getName(); 
			if (name.equals( xname )) {
				return p;
			}
		}
		return null;
	}

	@Override
	public void execute(Engine engine, NodeToken token) 
	{
		// first, close token set environment if this is ts-join node
		// - to avoid accidental values from tokenset environments:
		getJobProcessManager().closeTokensetEnvOnJoin( token );
		
		IJobAction action = getActionFactory().createAction( token );
		
		action = new JobActionReference( action );
		
		if (logger.isDebugEnabled()) {
			logger.debug( "Submitting " + action + "; parameters: "
				   + getJobProcessManager().getActionParameters( action, token ) );
		}
		getExecutor().submit( action, RUN );
	}

	@Transient
	private JobProcessManager getJobProcessManager() {
		return getActionFactory().getProcessManager();
	}
	@Transient
	private ActionExecutor getExecutor() {
		return ActionExecutorFactory.getActionExecutor();
	}
	
	@Transient
	private ActionFactory getActionFactory() {
		return ActionFactory.getInstance();
	}
	
}
