package com.trustwave.dbpjobservice.workflow.api.action;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Declare action property as an input parameter.</p>
 * <p>Input parameters are auto-populated by workflow engine from:</p>
 * <ul>
 * <li>workflow xml, if <i>value</i> attribute or <i>&lt;text&gt;</i>
 *     element is present in the corresponding <i>&lt;parameter&gt;</i>
 *     element</i>;</li>
 * <li>node token environment;</li>
 * <li>process environment (external parameter values provided by user).</li>
 * </ul>
 * The above order may be changed with <i>override</i> attributes.</p>
 * <p>Auto-population happens immediately after action instantiation,
 * before first invocation of begin() or checkCompleted() method.</p>
 * <p>Annotation may be applied to a field or set/get method of the
 *    action bean.</p>
 *
 * @author vlad
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface InputParameter {
    static final String DEFAULT_STR = "_N/A_";

    /**
     * Name used for displaying parameter in UI.
     */
    String displayName() default DEFAULT_STR;

    /**
     * Generic name of the parameter. Action parameters with the same
     * generic name are considered as different representations of
     * the same parameter.
     */
    String genericName() default DEFAULT_STR;

    /**
     * Parameter description to display in UI.
     */
    String description() default DEFAULT_STR;

    /**
     * Declare parameter as optional, i.e. absence of parameter value
     * will not be considered as error.
     * <p>NOTE: declaring parameter as optional does NOT exclude parameter from
     * the free parameters set; i.e. user may be prompted to provide value.</p>
     */
    boolean optional() default false;

    /**
     * Declares parameter as 'internal', i.e. excluded from the free parameters
     * set; user will not be prompted for it's value.
     * <p>NOTE: This attribute is logically equivalent to the
     * <i>valuePresent</i> attribute of the <i>&lt;parameter&gt;</i> element
     * in workflow XML.</p>
     */
    boolean internal() default false;

    /**
     * List of classes that provide parameter value validation.
     */
    Class<? extends ParameterValidator>[] validators() default {};

    /**
     * Marks parameter as a join parameter, i.e. the one that gathers results
     * from multiple execution lines.
     * <p>This attribute can be used only for parameters that implement
     * <i>List</i> or <i>Map</i> interface.</p>
     *
     * @see {@link OutputParameter#tokenSet()}.
     */
    boolean join() default false;

    /**
     * Name of parameter which value should be used as a key for Map join parameter.
     */
    String joinDiscriminator() default DEFAULT_STR;

    /**
     * If parameter is not externalizable (not a simple type or list of simple
     * types), force it to be externalizable as a string returned by parameter's
     * value converter.
     */
    boolean externalizableAsString() default false;
}
