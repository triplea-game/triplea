package games.strategy.engine.data.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that marks a method as a setter called by the GameParser (through the xml) and/or
 * PropertyUtil (through the ChangeFactory).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GameProperty {
  /**
   * Specifies that this property can be set by the GameParser and that it can appear in the map's XML file.
   *
   * @return true that this property identifies a setter that can be set by the GameParser (by the map's XML)
   */
  boolean xmlProperty();

  /**
   * Specifies that this property can be set by the PropertyUtil, which is done through the ChangeFactory.
   *
   * @return true that this property identifies a setter that can be set by the PropertyUtil (by the ChangeFactory)
   */
  boolean gameProperty();

  /**
   * Specifies that this property adds to a List or Map (ie: the List/Map/Object grows with each call of the 'set'
   * method, instead of being
   * overwritten and replaced each time).
   * Adders must have a 'clear' method, in addition to the usual get and set methods.
   *
   * @return true that this property identifies an adder instead of a setter
   */
  boolean adds();

  /**
   * Indicates the property is virtual and either has no backing field or uses one or more backing fields associated
   * with other non-virtual properties.
   *
   * <p>
   * A virtual property is typically used for so-called "convenience" properties that are used to initialize multiple
   * properties at once. Properties of this type are not required to have get, reset, or clear methods.
   * </p>
   *
   * @return {@code true} if the property is virtual; otherwise {@code false}.
   */
  boolean virtual() default false;
}
