package org.triplea.test.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a unit test that overrides the test object of another unit test and runs the same tests.
 * Typically will be named 'TestFileAsTestVariant.java' where 'TestFile' is the file under test,
 * will have a test named 'TestFileTest.java', and 'TestFileAsTestVariant' will extend
 * 'TestFileTest'.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExtendedUnitTest {}
