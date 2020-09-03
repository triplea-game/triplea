package org.triplea.generic.xml.reader;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Add this annotation to java fields to indicate their value should be set from an XML attribute.
 * The name of the attribute and the name of the field should match.
 *
 * <p>Example:
 *
 * <pre>{@code
 * class Tag {
 *   @Attribute
 *   private String attribute;
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Attribute {
  String defaultValue() default "";
}
