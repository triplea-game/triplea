package games.strategy.engine.data.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that marks a method setter or field as being only used within this class and any class that extends it,
 * and NOT being used by the GameParser (through the xml) and also not by PropertyUtil (through the
 * ChangeFactory).
 *
 * <p>
 * Do NOT export anything marked with this.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface InternalDoNotExport {
}
