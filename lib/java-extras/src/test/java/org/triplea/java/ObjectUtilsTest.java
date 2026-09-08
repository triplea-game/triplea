package org.triplea.java;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class ObjectUtilsTest {
  @Nested
  final class ReferenceEqualsTest {
    @Test
    void shouldReturnTrueWhenReferencesAreSame() {
      final Object a = new Object();

      assertThat(ObjectUtils.referenceEquals(null, null)).isTrue();
      assertThat(ObjectUtils.referenceEquals(a, a)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenReferencesAreNotSame() {
      final Object a = new Object();
      final Object b = new Object();

      assertThat(ObjectUtils.referenceEquals(a, null)).isFalse();
      assertThat(ObjectUtils.referenceEquals(null, a)).isFalse();
      assertThat(ObjectUtils.referenceEquals(a, b)).isFalse();
    }
  }
}
