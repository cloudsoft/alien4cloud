package alien4cloud.ui.form.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Label is shown on generic form instead of property name
 */
@Target({ FIELD, METHOD })
@Retention(RUNTIME)
public @interface FormLabel {
    String value();
}
