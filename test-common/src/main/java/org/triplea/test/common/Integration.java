package org.triplea.test.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;

/** Helper annotation to tag Tests as integration tests. Used to avoid typos. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Tag("integration")
public @interface Integration {
  TestType type() default TestType.INTEGRATION;
}
