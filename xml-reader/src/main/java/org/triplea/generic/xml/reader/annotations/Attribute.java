package org.triplea.generic.xml.reader.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Add this annotation to java fields to indicate their value should be mapped to an XML attribute.
 * The name of the XML attribute and the name of the Java field should match (case insensitive).
 *
 * <p>Example:
 *
 * <pre>{@code
 * class Tag {
 *   @Attribute
 *   private String attribute;
 * }
 * }</pre>
 *
 * With the XML below, the 'String attribute' above would get the value "attributeValue":
 *
 * <pre>{@code
 * <xml>
 *   <tag attribute="attributeValue" />
 * </xml>
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Attribute {
  String[] names() default "";

  String defaultValue() default "";

  int defaultInt() default 0;

  double defaultDouble() default 0.0;

  boolean defaultBoolean() default false;
}
