package org.triplea.java;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation indicating an unused element may be deleted on the next major release when we are okay
 * breaking save game compatibility.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface RemoveOnNextMajorRelease {
  String value() default "";
}
