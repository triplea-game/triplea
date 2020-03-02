package games.strategy.engine.message;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that sets the "opcode" for any particular method that may be used over the network
 * using the {@link RemoteMethodCall} mechanism. The integer doesn't carry any significant meaning,
 * but in order to stay compatible over the network it may not be changed at all. Any methods
 * signature of methods annotated with this annotation may not be altered if it should continue to
 * be compatible.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RemoteActionCode {
  int value();
}
