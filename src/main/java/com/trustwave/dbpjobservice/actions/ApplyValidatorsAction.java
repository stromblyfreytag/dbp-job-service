package com.trustwave.dbpjobservice.actions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.trustwave.dbpjobservice.workflow.api.action.InputParameter;
import com.trustwave.dbpjobservice.workflow.api.action.JobAction;
import com.trustwave.dbpjobservice.workflow.api.action.OutputParameter;
import com.trustwave.dbpjobservice.xml.XmlExitCondition;

/** <p>The action allows to execute all validators associated with
 * the specified parameter anywhere in the workflow,
 * with specified parameter value.</p>
 * <p>Particularly, validators may be applied after splitting job to multiple 
 * threads, to fail one thread instead of the whole job.</p>
 * <p>It is assumed that validators may work both with collection and
 *  single types - e.g. list of asset IDs and single ID; typical validator
 *  can do that</p>
 * <p>As a result, we have standard benefit of validators - early problem
 * detection in UI, plus flexibility provided by workflow - e.g. terminate only
 * offensive threads instead of the whole job.</p>
 * 
 *  
 * <p>Example of usage:</p>
 * <pre>{@code
 *    ....
 *    <parameter name="assetSet" ...>
 *       <validator class="...AssetOrgValidator" preventsExecution="false"/>
 *    </parameter>
 *    ....
 *    <node name='Apply Asset Validators' type='job-action'>
 *      <arc to='Next Action'/>
 *      <arc to='Finish' name='onValidationFailure'/>
 *      <custom><cs:attributes>"
 *        <cs:actionType class='com.appsec.jobservice.actions.ApplyValidatorsAction'/>
 *        <cs:parameter name='jobParameterName'  value='assetSet'/>
 *        <cs:parameter name='assetId' internalName='value' valuePresebt='true'/>
 *        <cs:output    name='validationErrors'/>
 *        <cs:selectArc name='onValidationFailure> unless='OK'/>
 *      </cs:attributes></custom>
 *    </node>
 *    ....
 * }</pre>
 * 
 * <p>NOTE: 'value' parameter should be specified as in the example above
 *  - by 'renaming' existing parameter (the one we are trying to validate).</p>
 *  
 * @author vlad
 *
 */
		
public class ApplyValidatorsAction extends JobAction
{
	@InputParameter( internal=true )
	private List<ValidationDescriptor> validationList;
	
	@InputParameter( internal=true )
	private String jobParameterName;
	
	@InputParameter( internal=true, optional=true )
	private boolean nonPreventiveOnly = false;
	
	// TODO: What we really need here is Object, not Serializable.
	// Unfortunately, there is no meaningful ValueConverter for Object,
	// and some ValueConverter is required.
	// We need either allow absence of ValueConverter for parameter
	// (e.g. with special attribute) or find some other way to pass
	// validated object value into action. 
	// Fortunately all supported 'simple' data types (string, int, list, etc)
	// are Serializable, so value is assigned directly, without conversion 
	@InputParameter( internal=true )
	private Serializable value;
	
	@OutputParameter
	private List<String> validationErrors;
	
	
	public boolean begin()
	{
		validationErrors = new ArrayList<String>();
		setExitCondition( null );
		
		for (ValidationDescriptor vd: validationList) {
			if (vd.getParameterName().equals(jobParameterName)) {
				
				if (isNonPreventiveOnly() && vd.isPreventsExecution()) {
					continue;
				}
				
				// use local copy of validation descriptor to avoid concurency problems
				ValidationDescriptor vd1 = new ValidationDescriptor( vd );
				if (!vd1.validate( getContext(), value )) {
					validationErrors.addAll( vd1.getErrors() );
					setCondition( XmlExitCondition.GENERAL_FAILURE,
							      vd1.getErrors().get(0) );
				}
			}
		}
		return true;
	}

	public List<ValidationDescriptor> getValidationList() {
		return validationList;
	}
	public void setValidationList(List<ValidationDescriptor> validationList) {
		this.validationList = validationList;
	}
	public void setJobParameterName(String parameterName) {
		this.jobParameterName = parameterName;
	}
	public void setValue(Serializable value) {
		this.value = value;
	}
	public List<String> getValidationErrors() {
		return validationErrors;
	}
	public void setNonPreventiveOnly(boolean nonPreventiveOnly) {
		this.nonPreventiveOnly = nonPreventiveOnly;
	}
	public boolean isNonPreventiveOnly() {
		return nonPreventiveOnly;
	}
}
