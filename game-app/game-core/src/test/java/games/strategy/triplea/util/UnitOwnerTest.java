package games.strategy.triplea.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
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
              GamePlayer.class,
              new GamePlayer("player1Name", gameData),
              new GamePlayer("player2Name", gameData))
          .withPrefabValues(
              UnitType.class,
              new UnitType("unitType1Name", gameData),
              new UnitType("unitType2Name", gameData))
          .verify();
    }
  }
}
