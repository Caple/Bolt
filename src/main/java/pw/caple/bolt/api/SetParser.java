package pw.caple.bolt.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sets this field as the class's type parser. This will allow the class to be
 * used as a parameter in Bolt protocol methods. This field must be static and
 * of the type TypeParser or an error will be thrown.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SetParser {

}
