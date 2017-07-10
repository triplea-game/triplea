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
  /**
   *  Should we auto save before the step starts?
   */
  boolean beforeStepStart() default false;

  /**
   *  Should we auto save after the step starts?
   *  Useful for the EndTurnDelegate in PBEM/PBF which allows the autosave before the "Done" button is clicked.
   *  Still a problem if the "User Report" is on though - doesn't save until "OK" is clicked.
   */
  boolean afterStepStart() default false;

  /**
   *  Should we auto save after the step starts?
   */
  boolean afterStepEnd() default false;
}
