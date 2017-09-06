package games.strategy.engine.data;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public final class FakeNamedUnitHolderTest {
  @Test
  public void shouldBeEquatableAndHashable() {
    EqualsVerifier.forClass(FakeNamedUnitHolder.class).verify();
  }
}
