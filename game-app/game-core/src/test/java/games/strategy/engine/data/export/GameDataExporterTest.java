package games.strategy.engine.data.export;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class GameDataExporterTest {
  @Nested
  final class ConnectionTest {
    @Nested
    final class EqualsAndHashCodeTest {
      @Test
      void shouldBeEquatableAndHashable() {
        final GameData gameData = new GameData();
        EqualsVerifier.forClass(GameDataExporter.Connection.class)
            .withPrefabValues(
                Territory.class,
                new Territory("redTerritory", gameData),
                new Territory("blackTerritory", gameData))
            .verify();
      }
    }
  }
}
