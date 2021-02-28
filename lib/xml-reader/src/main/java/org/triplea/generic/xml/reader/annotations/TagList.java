package org.triplea.generic.xml.reader.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TagList {
  /**
   * By default the searched for tag name will be be the name of annotated java object list generic
   * type. This property overrides that default to search for a different set of names.For example,
   * the below annotation would match tags named "foo" and "bar", but not "tag" .
   *
   * <pre>{@code
   * class Tag {
   *   @TagList(names = {"foo" , "bar"}
   *   private List<Tag> tagList;
   * }
   *
   * // matches
   * <list>
   *       <foo/>
   *       <bar/>
   *   </list>
   * }</pre>
   */
  String[] names() default "";
}
