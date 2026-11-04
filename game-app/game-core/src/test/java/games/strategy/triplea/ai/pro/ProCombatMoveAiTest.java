package games.strategy.triplea.ai.pro;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.removeFrom;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.unitType;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ai.pro.data.ProBattleResult;
import games.strategy.triplea.ai.pro.data.ProTerritory;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProCombatMoveAiTest {
  private final GameData gameData = TestMapGameData.LHTR.getGameData();

  @Test
  void smallLandingThatCantBeHeldIsNotValuable() {
    final ProTerritory newGuinea =
        new ProTerritory(territory("New Guinea", gameData), new ProData());
    newGuinea.setCanHold(false);

    assertThat(ProCombatMoveAi.isValuableLanding(newGuinea, true)).isFalse();
  }

  @Test
  void smallLandingThatCanBeHeldIsValuable() {
    final ProTerritory newGuinea =
        new ProTerritory(territory("New Guinea", gameData), new ProData());
    newGuinea.setCanHold(true);

    assertThat(ProCombatMoveAi.isValuableLanding(newGuinea, true)).isTrue();
  }

  @Test
  void factoryThatCantBeHeldAndLosesTuvIsValuableOnHard() {
    final ProTerritory southernEurope =
        new ProTerritory(territory("Southern Europe", gameData), new ProData());
    southernEurope.setCanHold(false);
    southernEurope.setBattleResult(battleResult(99, -5, true));

    assertThat(ProCombatMoveAi.isValuableLanding(southernEurope, false)).isTrue();
  }

  @Test
  void factoryThatCantBeHeldAndLosesTuvIsNotValuableOnBeta() {
    final ProTerritory southernEurope =
        new ProTerritory(territory("Southern Europe", gameData), new ProData());
    southernEurope.setCanHold(false);
    southernEurope.setBattleResult(battleResult(99, -5, true));

    assertThat(ProCombatMoveAi.isValuableLanding(southernEurope, true)).isFalse();
  }

  @Test
  void factoryThatCantBeHeldButGainsTuvIsValuable() {
    final ProTerritory southernEurope =
        new ProTerritory(territory("Southern Europe", gameData), new ProData());
    southernEurope.setCanHold(false);
    southernEurope.setBattleResult(battleResult(99, 5, true));

    assertThat(ProCombatMoveAi.isValuableLanding(southernEurope, true)).isTrue();
  }

  @Test
  void factoryThatCanBeHeldIsValuable() {
    final ProTerritory southernEurope =
        new ProTerritory(territory("Southern Europe", gameData), new ProData());
    southernEurope.setCanHold(true);

    assertThat(ProCombatMoveAi.isValuableLanding(southernEurope, true)).isTrue();
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

  @Test
  void planeGoesToTheMostValuableAttackThatIsNotYetSure() {
    final ProTerritory ukraine = attack("Ukraine S.S.R.", 21, 70);
    final ProTerritory karelia = attack("Karelia S.S.R.", 10, 50);
    final ProTerritory westRussia = attack("West Russia", 58, 97);

    assertThat(ProCombatMoveAi.mostValuableUnsureAttack(List.of(karelia, westRussia, ukraine), 90))
        .contains(ukraine);
  }

  @Test
  void noPickWhenEveryAttackIsSure() {
    final ProTerritory westRussia = attack("West Russia", 58, 97);

    assertThat(ProCombatMoveAi.mostValuableUnsureAttack(List.of(westRussia), 90)).isEmpty();
  }

  private ProTerritory attack(
      final String name, final double value, final double capturePercentage) {
    final ProTerritory attack = new ProTerritory(territory(name, gameData), new ProData());
    attack.setValue(value);
    attack.setBattleResult(
        new ProBattleResult(
            capturePercentage, 1, true, List.of(), List.of(), 1, capturePercentage));
    return attack;
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
  void tradingAnOwnerHeldCapitalIsWorthItsIncomeAndTreasury() {
    // Germany produces 10 with a factory; the Germans make 40 a turn and hold 40 PUs.
    assertThat(ProCombatMoveAi.tradedTerritoryWorth(territory("Germany", gameData), gameData))
        .isEqualTo((2 + 1) * 10 + 40 + 40);
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

  @Test
  void alliedCapitalHeldByTheEnemyThatCanBeHeldIsBoostedAsATarget() {
    territory("Russia", gameData).setOwner(germans(gameData));
    final ProTerritory russia = new ProTerritory(territory("Russia", gameData), new ProData());
    russia.setCanHold(true);

    assertThat(ProCombatMoveAi.isCapitalWorthTargeting(russia, british(gameData), gameData))
        .isTrue();
  }

  @Test
  void alliedCapitalHeldByTheEnemyThatCantBeHeldIsNotBoostedAsATarget() {
    territory("Russia", gameData).setOwner(germans(gameData));
    final ProTerritory russia = new ProTerritory(territory("Russia", gameData), new ProData());
    russia.setCanHold(false);

    assertThat(ProCombatMoveAi.isCapitalWorthTargeting(russia, british(gameData), gameData))
        .isFalse();
  }

  @Test
  void unloadNextToEnemySubWithoutEscortIsBlocked() {
    final GameData g40 = TestMapGameData.GLOBAL1940.getGameData();
    final Territory germany = territory("Germany", g40);
    final Territory zone = seaZoneNextTo(germany, g40);
    removeFrom(zone, zone.getUnits());
    addTo(zone, unitType("submarine", g40).create(1, germans(g40)));

    assertThat(ProCombatMoveAi.isUnloadBlockedByEnemySubs(british(g40), germany, zone, List.of()))
        .isTrue();
  }

  @Test
  void unloadNextToEnemySubIsAllowedWithAWarshipPresentOrMovingIn() {
    final GameData g40 = TestMapGameData.GLOBAL1940.getGameData();
    final Territory germany = territory("Germany", g40);
    final Territory zone = seaZoneNextTo(germany, g40);
    removeFrom(zone, zone.getUnits());
    addTo(zone, unitType("submarine", g40).create(1, germans(g40)));
    final var destroyer = unitType("destroyer", g40).create(1, british(g40));

    assertThat(ProCombatMoveAi.isUnloadBlockedByEnemySubs(british(g40), germany, zone, destroyer))
        .isFalse();
    addTo(zone, destroyer);
    assertThat(ProCombatMoveAi.isUnloadBlockedByEnemySubs(british(g40), germany, zone, List.of()))
        .isFalse();
  }

  @Test
  void unloadIsNotBlockedWithoutSubsOrOntoAFriendlyTerritory() {
    final GameData g40 = TestMapGameData.GLOBAL1940.getGameData();
    final Territory germany = territory("Germany", g40);
    final Territory zone = seaZoneNextTo(germany, g40);
    removeFrom(zone, zone.getUnits());

    assertThat(ProCombatMoveAi.isUnloadBlockedByEnemySubs(british(g40), germany, zone, List.of()))
        .isFalse();

    addTo(zone, unitType("submarine", g40).create(1, germans(g40)));
    final Territory ownLand = territory("United Kingdom", g40);
    assertThat(ProCombatMoveAi.isUnloadBlockedByEnemySubs(british(g40), ownLand, zone, List.of()))
        .isFalse();
  }

  @Test
  void findsOnlyTheLandingsWhoseUnloadZoneHoldsUnescortedEnemySubs() {
    final GameData g40 = TestMapGameData.GLOBAL1940.getGameData();
    final Territory germany = territory("Germany", g40);
    final Territory zone = seaZoneNextTo(germany, g40);
    removeFrom(zone, zone.getUnits());
    addTo(zone, unitType("submarine", g40).create(1, germans(g40)));
    final Unit transport = unitType("transport", g40).create(1, british(g40)).iterator().next();
    final Unit infantry = unitType("infantry", g40).create(1, british(g40)).iterator().next();
    final ProTerritory blocked = new ProTerritory(germany, new ProData());
    blocked.getAmphibAttackMap().put(transport, List.of(infantry));
    blocked.getTransportTerritoryMap().put(transport, zone);
    final ProTerritory noAmphib = new ProTerritory(territory("France", g40), new ProData());

    final var attackMap = Map.of(germany, blocked, territory("France", g40), noAmphib);

    assertThat(ProCombatMoveAi.findLandingsBlockedByEnemySubs(british(g40), attackMap))
        .containsExactly(blocked);
  }

  private static Territory seaZoneNextTo(final Territory land, final GameData data) {
    return data.getMap().getNeighbors(land, Matches.territoryIsWater()).iterator().next();
  }
}
