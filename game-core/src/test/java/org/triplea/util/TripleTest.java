package org.triplea.util;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class TripleTest {
  @Nested
  final class EqualsAndHashCodeTest {
    @Test
    void shouldBeEquatableAndHashable() {
      EqualsVerifier.forClass(Triple.class).verify();
    }
  }
}
