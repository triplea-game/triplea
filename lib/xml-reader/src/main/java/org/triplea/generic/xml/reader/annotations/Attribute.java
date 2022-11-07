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
  /**
   * By default the searched for attribute name will be be the name of annotated java field. This
   * property overrides that default to search for a different set of names.For example, the below
   * annotation would match attributes "foo" and "bar", but not "attribute".
   *
   * <pre>{@code
   * class Tag {
   *   @Attribute(names = {"foo" , "bar"}
   *   private String attribute;
   * }
   * }</pre>
   */
  String[] names() default "";

  String defaultValue() default "";

  int defaultInt() default 0;

  double defaultDouble() default 0.0;

  boolean defaultBoolean() default false;
}
