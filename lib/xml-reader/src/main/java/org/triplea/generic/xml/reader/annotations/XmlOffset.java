package org.triplea.generic.xml.reader.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Add this annotation to java fields to indicate their value should be mapped to the offset of the
 * element in the XML file. This can be useful when a parent element supports different types of
 * child elements and you need to know their relative order.
 *
 * <p>Example:
 *
 * <pre>{@code
 * class Tag {
 *   @XmlOffset
 *   private int xmlOffset;
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface XmlOffset {}
