package games.strategy.triplea.ai.pro;

import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.MutableProperty;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ai.pro.data.ProBattleResult;
import games.strategy.triplea.ai.pro.data.ProTerritory;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProCombatMoveAiTest {
  private final GameData gameData = TestMapGameData.LHTR.getGameData();

  @Test
  void smallLandingThatCantBeHeldIsNotValuable() {
    final ProTerritory newGuinea =
        new ProTerritory(territory("New Guinea", gameData), new ProData());
    newGuinea.setCanHold(false);

    assertThat(ProCombatMoveAi.isValuableLanding(newGuinea)).isFalse();
  }

  @Test
  void smallLandingThatCanBeHeldIsValuable() {
    final ProTerritory newGuinea =
        new ProTerritory(territory("New Guinea", gameData), new ProData());
    newGuinea.setCanHold(true);

    assertThat(ProCombatMoveAi.isValuableLanding(newGuinea)).isTrue();
  }

  @Test
  void factoryIsValuableEvenIfItCantBeHeld() {
    final ProTerritory southernEurope =
        new ProTerritory(territory("Southern Europe", gameData), new ProData());
    southernEurope.setCanHold(false);

    assertThat(ProCombatMoveAi.isValuableLanding(southernEurope)).isTrue();
  }

  @Test
  void bomberIsSparedWhenAttackStillWinsAndRaidOutweighsLostSwing() {
    final ProBattleResult withBomber = battleResult(99, 10, true);
    final ProBattleResult withoutBomber = battleResult(97, 8, true);

    assertThat(ProCombatMoveAi.canSpareBomberForRaid(withBomber, withoutBomber, 95, 3)).isTrue();
  }

  @Test
  void bomberIsKeptWhenAttackNoLongerWins() {
    final ProBattleResult withBomber = battleResult(96, 10, true);
    final ProBattleResult withoutBomber = battleResult(90, 9, true);

    assertThat(ProCombatMoveAi.canSpareBomberForRaid(withBomber, withoutBomber, 95, 3)).isFalse();
  }

  @Test
  void bomberIsKeptWhenNoLandUnitWouldSurviveToCapture() {
    final ProBattleResult withBomber = battleResult(99, 10, true);
    final ProBattleResult withoutBomber = battleResult(99, 10, false);

    assertThat(ProCombatMoveAi.canSpareBomberForRaid(withBomber, withoutBomber, 95, 3)).isFalse();
  }

  @Test
  void bomberIsKeptWhenLostSwingExceedsRaidDamage() {
    final ProBattleResult withBomber = battleResult(99, 10, true);
    final ProBattleResult withoutBomber = battleResult(99, 6, true);

    assertThat(ProCombatMoveAi.canSpareBomberForRaid(withBomber, withoutBomber, 95, 3)).isFalse();
  }

  private static ProBattleResult battleResult(
      final double winPercentage, final double tuvSwing, final boolean hasLandUnitRemaining) {
    return new ProBattleResult(
        winPercentage, tuvSwing, hasLandUnitRemaining, List.of(), List.of(), 1);
  }

  @Test
  void tradingACapitalHeldByItsCaptorIsWorthNoCaptorIncome() {
    territory("Russia", gameData).setOwner(germans(gameData));

    assertThat(ProCombatMoveAi.tradedTerritoryWorth(territory("Russia", gameData), gameData))
        .isEqualTo((2 + 1) * 8);
  }

  @Test
  void enemyCapitalWhoseCaptureStopsItsIncomeIsBoosted() {
    assertThat(
            ProCombatMoveAi.isCapitalWorthBoosting(
                territory("Germany", gameData), british(gameData), gameData))
        .isTrue();
  }

  @Test
  void enemyCapitalWhoseCaptureKeepsItsIncomeIsNotBoosted()
      throws MutableProperty.InvalidValueException {
    final PlayerAttachment pa =
        new PlayerAttachment(Constants.PLAYER_ATTACHMENT_NAME, germans(gameData), gameData);
    pa.getPropertyOrThrow("retainCapitalProduceNumber").setValue(0);
    germans(gameData).addAttachment(Constants.PLAYER_ATTACHMENT_NAME, pa);

    assertThat(
            ProCombatMoveAi.isCapitalWorthBoosting(
                territory("Germany", gameData), british(gameData), gameData))
        .isFalse();
  }

  @Test
  void alliedCapitalHeldByTheEnemyIsBoosted() {
    territory("Russia", gameData).setOwner(germans(gameData));

    assertThat(
            ProCombatMoveAi.isCapitalWorthBoosting(
                territory("Russia", gameData), british(gameData), gameData))
        .isTrue();
  }

  @Test
  void ownCapitalWhoseLossKeepsItsIncomeIsNotDefendedAsCapital()
      throws MutableProperty.InvalidValueException {
    final PlayerAttachment pa =
        new PlayerAttachment(Constants.PLAYER_ATTACHMENT_NAME, british(gameData), gameData);
    pa.getPropertyOrThrow("retainCapitalProduceNumber").setValue(0);
    british(gameData).addAttachment(Constants.PLAYER_ATTACHMENT_NAME, pa);

    assertThat(
            ProCombatMoveAi.isCapitalWorthDefending(
                territory("United Kingdom", gameData), british(gameData), gameData))
        .isFalse();
  }

  @Test
  void enemyCapitalHeldByUsIsDefendedAsCapital() {
    territory("Germany", gameData).setOwner(british(gameData));

    assertThat(
            ProCombatMoveAi.isCapitalWorthDefending(
                territory("Germany", gameData), british(gameData), gameData))
        .isTrue();
  }
}
