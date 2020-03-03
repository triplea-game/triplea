package games.strategy.engine.message;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that defines a method number, methods are called by method number using the {@link
 * RemoteMethodCall} mechanism}". In order to stay compatible over the network, annotated methods'
 * signature should not be altered (including: changes in signature, or changes in the method
 * number).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RemoteActionCode {
  int value();
}
