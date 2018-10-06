package org.triplea.test.common.assertions;

import java.util.Optional;

import org.hamcrest.Matcher;
import org.triplea.test.common.CustomMatcher;

/**
 * Utility class with hamcrest matchers for {@code java.util.Optional}.
 */
public final class Optionals {

  private Optionals() {

  }

  public static <T> Matcher<Optional<T>> isMissing() {
    return CustomMatcher.<Optional<T>>builder()
        .description("Expecting an empty Optional")
        .checkCondition(optional -> !optional.isPresent())
        .debug(optional -> "Was non-empty Optional containing: " + optional
            .orElseThrow(() -> new AssertionError("Optional should have been non-empty")))
        .build();
  }

  public static <T> Matcher<Optional<T>> isPresent() {
    return CustomMatcher.<Optional<T>>builder()
        .description("Expecting a non-empty Optional")
        .checkCondition(Optional::isPresent)
        .build();
  }
}
