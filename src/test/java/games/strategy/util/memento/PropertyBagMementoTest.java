package games.strategy.util.memento;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public final class PropertyBagMementoTest {
  @Test
  public void shouldBeEquatableAndHashable() {
    EqualsVerifier.forClass(PropertyBagMemento.class)
        .suppress(Warning.NULL_FIELDS)
        .verify();
  }
}
