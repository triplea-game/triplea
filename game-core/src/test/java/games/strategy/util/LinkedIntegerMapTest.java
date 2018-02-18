package games.strategy.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public final class LinkedIntegerMapTest {
  private final Object key = new Object();

  @Test
  public void shouldBeEquatableAndHashable() {
    EqualsVerifier.forClass(LinkedIntegerMap.class)
        .suppress(Warning.NULL_FIELDS)
        .verify();

    // We need to explicitly test this case because EqualsVerifier's internal prefab values for LinkedHashMap use the
    // same value for all key/value pairs
    assertThat(
        "should not be equal when keys are equal but values are not equal",
        new LinkedIntegerMap<>(key, 1),
        is(not(new LinkedIntegerMap<>(key, 2))));
  }
}
