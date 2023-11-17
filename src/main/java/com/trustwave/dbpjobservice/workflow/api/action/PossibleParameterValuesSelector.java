package com.trustwave.dbpjobservice.workflow.api.action;

import java.util.List;

/**
 * <p>This interface should be implemented by selectors of possible parameter
 * values; i.e. objects that provide a list of possible parameter values.
 * </p>
 * <p>The main method of selector is selectPossibleValues() call, that should
 * return a list of key-value pairs where key represents possible value,
 * and keyed value typically is a display string corresponding to the key.
 * </p>
 * <p>Also selector should be java bean, i.e. should have public no-arg
 * constructor and optionally properties with public getters and setters.
 * Bean properties may be used to customize behavior of the selector.
 * They will be populated by Job Service from the selector description
 * in the workflow.
 * </p>
 * <p>Selector object has the following lifecycle:
 * <ol><li>Object creation (with default constructor)</li>
 *     <li>Populate object bean properties, if any, with values specified
 *         in the selector descriptor in the workflow</li>
 *     <li>Call init() method with job context as a parameter</li>
 *     <li>Call selectPossibleValues() method with the target parameter name.</li>
 * </ol>
 * After that selector object is disposed (not used any more).
 * Every instance of selector object is used only once, so it does not make sense
 * to organize internal caches, e.g for selected values.
 * </p>
 *
 * @author vlad
 */
public interface PossibleParameterValuesSelector {
    /**
     * <p>This method will be called after creation of selector instance
     * and setting optional arguments, before selectPossibleValues() call.
     * </p>
     * <p> Typical selector will just save the context to use it later
     * in the selectPossibleValues() call.
     * </p>
     *
     * @param context Job context.
     */
    public void init(JobContext context);

    /**
     * <p>The method should return a list of possible parameter values
     * mapped to their display strings, properly ordered.</p>
     * <p>KeyValuePair entry should contain:</p>
     * <ul>
     * <li><b>key:</b>   possible parameter value (as used in the job), e.g. policy ID</li>
     * <li><b>value:</b> corresponding human-readable display string, e.g. policy name</li>
     * </ul>
     * <p>Key and value may be the same, of course.
     * Just remember that values will be used in UI (and only in UI), e.g. for drop-down;
     * while keys will be used in job as parameter values.</p>
     *
     * @param parameterName
     * @return
     */
    public List<KeyValuePair> selectPossibleValues(String parameterName);
}
