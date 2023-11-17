package com.trustwave.dbpjobservice.workflow.api.action;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Declare action property as an output parameter.</p>
 * <p>Output parameters are auto-saved in the node token environment
 * after every action execution phase (begin(), checkCompleted(), or cancel()).</p>
 * <p>Annotation may be applied to a field or set/get method of the
 * action bean.</p>
 *
 * @author vlad
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface OutputParameter {
    static final String DEFAULT_STR = "_N/A_";

    String tokenSet() default DEFAULT_STR;
}
