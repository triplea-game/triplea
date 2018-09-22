package games.strategy.triplea.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.UnitType;
import nl.jqno.equalsverifier.EqualsVerifier;

final class UnitOwnerTest {
  @Nested
  final class EqualsAndHashCodeTest {
    @Test
    void shouldBeEquatableAndHashable() {
      final GameData gameData = new GameData();
      EqualsVerifier.forClass(UnitOwner.class)
          .withPrefabValues(
              PlayerID.class,
              new PlayerID("player1Name", gameData),
              new PlayerID("player2Name", gameData))
          .withPrefabValues(
              UnitType.class,
              new UnitType("unitType1Name", gameData),
              new UnitType("unitType2Name", gameData))
          .verify();
    }
  }
}
