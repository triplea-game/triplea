package org.triplea.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TupleTest {
  private final Tuple<String, Integer> testObj = Tuple.of("hi", 123);

  @Test
  void basicUsage() {
    assertThat(testObj.getFirst(), is("hi"));
    assertThat(testObj.getSecond(), is(123));
  }

  @Test
  void verifyToString() {
    assertThat(testObj.toString(), containsString(testObj.getFirst()));
    assertThat(testObj.toString(), containsString(String.valueOf(testObj.getSecond())));
  }

  @Test
  void checkStoringNullCase() {
    final Tuple<String, String> nullTuple = Tuple.of(null, null);

    assertThat(nullTuple.getFirst(), nullValue());
    assertThat(nullTuple.getSecond(), nullValue());
    assertThat(nullTuple, not(Tuple.of("something else", (String) null)));
  }

  @Test
  void checkSwap() {
    final Tuple<String, Integer> t1 = Tuple.of("hi", 123);
    final Tuple<Integer, String> t2 = t1.swap();
    assertThat(t2.getFirst(), is(123));
    assertThat(t2.getSecond(), is("hi"));
  }

  @Nested
  final class EqualsAndHashCodeTest {
    @Test
    void shouldBeEquatableAndHashable() {
      EqualsVerifier.forClass(Tuple.class).verify();
    }
  }
}
