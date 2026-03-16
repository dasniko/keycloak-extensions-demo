package de.keycloak.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

@Documented
@Retention(SOURCE)
@Target({TYPE, METHOD, FIELD, CONSTRUCTOR})
public @interface CopiedFromKeycloak {
    /**
     * Original class or file in Keycloak.
     * Example: "org.keycloak.common.util.Time"
     */
    String source() default "";

    /**
     * Keycloak version the code was copied from.
     * Example: "26.5.5"
     */
    String version() default "";

    /**
     * Whether the copied code was modified.
     */
    boolean modified() default false;

    /**
     * Description of the modifications, if any.
     */
    String changes() default "";

    /**
     * Reason why the code was copied instead of reused.
     */
    String reason() default "";

    /**
     * Optional date when the code was copied.
     * Example: "2026-03-16"
     */
    String copiedAt() default "";
}
