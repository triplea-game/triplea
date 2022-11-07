package org.triplea.generic.xml.reader.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Add this annotation to java fields to indicate their value should be mapped to the body text of
 * an XML attribute. The name of such a field can be anything.
 *
 * <p>Example:
 *
 * <pre>{@code
 * class Tag {
 *   @BodyText
 *   private String content;
 * }
 * }</pre>
 *
 * With the XML below, the 'String content attribute' above would get the value "body text":
 *
 * <pre>{@code
 * <xml>
 *   <tag>body text</tag>
 * </xml>
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface BodyText {}
