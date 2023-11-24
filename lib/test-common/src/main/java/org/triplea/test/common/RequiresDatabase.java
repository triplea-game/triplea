package org.triplea.test.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;

/**
 * 'RequiresDatabase' means a database needs to be running (presumably on docker) for the test to
 * pass. Tests annotated with 'RequiresDatabase' will not run with 'gradle test' task but will
 * instead run with the 'check', 'testAll' or 'testWithDatabase' tasks.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Tag("RequiresDatabase")
public @interface RequiresDatabase {}
