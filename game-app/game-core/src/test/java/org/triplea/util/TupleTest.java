package org.triplea.util;

import static org.assertj.core.api.Assertions.assertThat;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TupleTest {
  private final Tuple<String, Integer> testObj = Tuple.of("hi", 123);

  @Test
  void basicUsage() {
    assertThat(testObj.getFirst()).isEqualTo("hi");
    assertThat(testObj.getSecond()).isEqualTo(123);
  }

  @Test
  void verifyToString() {
    assertThat(testObj.toString()).contains(testObj.getFirst());
    assertThat(testObj.toString()).contains(String.valueOf(testObj.getSecond()));
  }

  @Test
  void checkStoringNullCase() {
    final Tuple<String, String> nullTuple = Tuple.of(null, null);

    assertThat(nullTuple.getFirst()).isNull();
    assertThat(nullTuple.getSecond()).isNull();
    assertThat(nullTuple).isNotEqualTo(Tuple.of("something else", (String) null));
  }

  @Nested
  final class EqualsAndHashCodeTest {
    @Test
    void shouldBeEquatableAndHashable() {
      EqualsVerifier.forClass(Tuple.class).verify();
    }
  }
}
