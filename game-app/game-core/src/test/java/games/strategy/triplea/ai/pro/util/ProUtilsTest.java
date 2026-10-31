package games.strategy.triplea.ai.pro.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.japanese;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProUtilsTest {

  @Test
  void testIsPassiveNeutralPlayer() {
    final GameState data = TestMapGameData.GLOBAL1940.getGameData();
    final GamePlayer russians = data.getPlayerList().getPlayerId("Russians");
    assertFalse(ProUtils.isPassiveNeutralPlayer(russians));
    final GamePlayer neutralTrue = data.getPlayerList().getPlayerId("Neutral_True");
    assertTrue(ProUtils.isPassiveNeutralPlayer(neutralTrue));
    final GamePlayer pirates = data.getPlayerList().getPlayerId("Pirates");
    assertFalse(ProUtils.isPassiveNeutralPlayer(pirates));
  }

  @Test
  void testIsNeutralPlayer() {
    final GameState data = TestMapGameData.GLOBAL1940.getGameData();
    final GamePlayer russians = data.getPlayerList().getPlayerId("Russians");
    assertFalse(ProUtils.isNeutralPlayer(russians));
    final GamePlayer neutralTrue = data.getPlayerList().getPlayerId("Neutral_True");
    assertTrue(ProUtils.isNeutralPlayer(neutralTrue));
    final GamePlayer pirates = data.getPlayerList().getPlayerId("Pirates");
    assertTrue(ProUtils.isNeutralPlayer(pirates));
  }

  @Test
  void testSummarizeUnits() {
    final GameState data = TestMapGameData.GLOBAL1940.getGameData();
    final List<Unit> units = new ArrayList<>();
    units.addAll(infantry(data).create(5, germans(data)));
    units.addAll(armour(data).create(1, germans(data)));
    units.addAll(infantry(data).create(2, japanese(data)));
    assertThat(ProUtils.summarizeUnits(units))
        .isEqualTo(
            "[armour owned by Germans, 5 infantry owned by Germans, 2 infantry owned by"
                + " Japanese]");
  }

  @Test
  void takingTheOwnersOnlyCapitalStopsItsIncome() {
    final GameData data = TestMapGameData.LHTR.getGameData();

    assertThat(ProUtils.captureStopsIncome(territory("Germany", data), data.getMap())).isTrue();
  }

  @Test
  void takingOneOfTwoCapitalsKeepsTheOwnersIncome() throws GameParseException {
    final GameData data = TestMapGameData.LHTR.getGameData();
    TerritoryAttachment.getOrThrow(territory("Southern Europe", data)).setCapital("Germans");

    assertThat(ProUtils.captureStopsIncome(territory("Germany", data), data.getMap())).isFalse();
  }

  @Test
  void takingOneOfTwoCapitalsTakesTheTreasuryOfAnOwnerWithoutPlayerAttachment()
      throws GameParseException {
    final GameData data = TestMapGameData.LHTR.getGameData();
    TerritoryAttachment.getOrThrow(territory("Southern Europe", data)).setCapital("Germans");

    assertThat(ProUtils.captureTakesTreasury(territory("Germany", data), data.getMap())).isTrue();
  }

  @Test
  void takingACapitalFromItsCaptorStopsNoIncome() {
    final GameData data = TestMapGameData.LHTR.getGameData();
    territory("Russia", data).setOwner(germans(data));

    assertThat(ProUtils.captureStopsIncome(territory("Russia", data), data.getMap())).isFalse();
  }

  @Test
  void takingOneOfTwoCapitalsIsWorthNoOwnerIncome() throws GameParseException {
    final GameData data = TestMapGameData.LHTR.getGameData();
    TerritoryAttachment.getOrThrow(territory("Southern Europe", data)).setCapital("Germans");

    assertThat(ProUtils.capitalValue(territory("Germany", data), data)).isZero();
  }
}
