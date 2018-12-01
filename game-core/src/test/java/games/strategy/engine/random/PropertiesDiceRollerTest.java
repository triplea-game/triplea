package games.strategy.engine.random;

import java.util.Properties;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

final class PropertiesDiceRollerTest {
  @Nested
  final class EqualsAndHashCodeTest {
    @Test
    void shouldBeEquatableAndHashable() {
      EqualsVerifier.forClass(PropertiesDiceRoller.class)
          .withOnlyTheseFields("props")
          .withPrefabValues(Properties.class, newProperties("red"), newProperties("black"))
          .verify();
    }
    
    private Properties newProperties(final String name) {
      final Properties properties = new Properties();
      properties.put(PropertiesDiceRoller.PropertyKeys.NAME, name);
      return properties;
    }
  }
}
