package com.trustwave.dbpjobservice.workflow.api.action;

import java.util.List;

public interface ParameterValidator {
    /**
     * <p>This method will be called immediately after creation of validator
     * instance, before call to validate.
     * </p>
     * <p> Typical validator will just save the context to use it later
     * in the validate() call.
     * </p>
     *
     * @param context Job context.
     */
    public void init(JobContext context);

    /**
     * <p>This method is called to validate parameter value. Discovered problems
     * (if any) should be added to the 'errors' list, one string per problem.
     * </p>
     * <p> This method will not be invoked when parameter value is null or empty;
     * validators that need to be invoked with such values should be annotated with
     * {@link @NullValueSafe} annotation, indicating they know how to handle null parameters.
     * </p>
     *
     * @param paramValue parameter value to validate.
     * @param paramName TODO
     * @param errors errors holder
     */
    public void validate(Object paramValue, String paramName, List<String> errors);
}
