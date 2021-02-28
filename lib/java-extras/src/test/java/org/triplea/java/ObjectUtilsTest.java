package org.triplea.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class ObjectUtilsTest {
  @Nested
  final class ReferenceEqualsTest {
    @Test
    void shouldReturnTrueWhenReferencesAreSame() {
      final Object a = new Object();

      assertThat(ObjectUtils.referenceEquals(null, null), is(true));
      assertThat(ObjectUtils.referenceEquals(a, a), is(true));
    }

    @Test
    void shouldReturnFalseWhenReferencesAreNotSame() {
      final Object a = new Object();
      final Object b = new Object();

      assertThat(ObjectUtils.referenceEquals(a, null), is(false));
      assertThat(ObjectUtils.referenceEquals(null, a), is(false));
      assertThat(ObjectUtils.referenceEquals(a, b), is(false));
    }
  }
}
