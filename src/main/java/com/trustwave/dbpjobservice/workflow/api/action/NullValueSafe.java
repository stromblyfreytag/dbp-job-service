package com.trustwave.dbpjobservice.workflow.api.action;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for {@link ParameterValidator} classes indicating they know
 * how to handle null/empty parameter values.
 *
 * @author vaverchenkov
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface NullValueSafe {
}
