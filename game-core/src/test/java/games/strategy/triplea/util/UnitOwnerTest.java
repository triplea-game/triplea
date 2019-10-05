package games.strategy.triplea.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.UnitType;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class UnitOwnerTest {
  @Nested
  final class EqualsAndHashCodeTest {
    @Test
    void shouldBeEquatableAndHashable() {
      final GameData gameData = new GameData();
      EqualsVerifier.forClass(UnitOwner.class)
          .withPrefabValues(
              PlayerId.class,
              new PlayerId("player1Name", gameData),
              new PlayerId("player2Name", gameData))
          .withPrefabValues(
              UnitType.class,
              new UnitType("unitType1Name", gameData),
              new UnitType("unitType2Name", gameData))
          .verify();
    }
  }
}
