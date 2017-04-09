package games.strategy.engine.delegate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Delegates with this annotation will trigger the autosave when they start or end.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoSave {
  // should we auto save before the step starts
  boolean beforeStepStart() default false;

  // should we auto save after the step ends
  boolean afterStepEnd() default false;
}
