package games.strategy.triplea.ai.pro;

import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.ai.pro.data.ProTerritory;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

class ProCombatMoveAiTest {
  private final GameData gameData = TestMapGameData.LHTR.getGameData();

  @Test
  void smallLandingThatCantBeHeldIsNotWorthReAdding() {
    final ProTerritory newGuinea =
        new ProTerritory(territory("New Guinea", gameData), new ProData());
    newGuinea.setCanHold(false);

    assertThat(ProCombatMoveAi.isWorthReAdding(newGuinea)).isFalse();
  }

  @Test
  void smallLandingThatCanBeHeldIsWorthReAdding() {
    final ProTerritory newGuinea =
        new ProTerritory(territory("New Guinea", gameData), new ProData());
    newGuinea.setCanHold(true);

    assertThat(ProCombatMoveAi.isWorthReAdding(newGuinea)).isTrue();
  }

  @Test
  void factoryIsWorthReAddingEvenIfItCantBeHeld() {
    final ProTerritory southernEurope =
        new ProTerritory(territory("Southern Europe", gameData), new ProData());
    southernEurope.setCanHold(false);

    assertThat(ProCombatMoveAi.isWorthReAdding(southernEurope)).isTrue();
  }

  @Test
  void takingOneOfTwoCapitalsIsWorthNoOwnerIncome() throws GameParseException {
    TerritoryAttachment.getOrThrow(territory("Southern Europe", gameData)).setCapital("Germans");

    assertThat(ProCombatMoveAi.capitalValue(territory("Germany", gameData), gameData)).isZero();
  }
}
