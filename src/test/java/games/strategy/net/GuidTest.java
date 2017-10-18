package games.strategy.net;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public final class GuidTest {
  @Test
  public void shouldBeEquatableAndHashable() {
    EqualsVerifier.forClass(GUID.class)
        .suppress(Warning.NONFINAL_FIELDS, Warning.NULL_FIELDS)
        .verify();
  }
}
