package games.strategy.triplea.ai.pro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import games.strategy.triplea.ai.pro.data.ProBattleResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProScoringTest {
  private static final double TOLERANCE = 1e-9;

  private static ProScoring.AttackTarget landTargetWorth(
      final double tuvSwing, final int production) {
    return new ProScoring.AttackTarget(
        tuvSwing, production, true, false, false, false, false, false, false, false, false);
  }

  @Test
  void attackPriorityOfAPlainLandTargetAddsSwingToTwiceProduction() {
    assertThat(ProScoring.attackPriority(landTargetWorth(5, 3))).isCloseTo(11, within(TOLERANCE));
  }

  @Test
  void attackPriorityHalvesGainsInAFreeForAllButNotLosses() {
    final var gain =
        new ProScoring.AttackTarget(
            10, 0, true, false, false, false, false, false, false, true, false);
    final var loss =
        new ProScoring.AttackTarget(
            -10, 0, true, false, false, false, false, false, false, true, false);

    assertThat(ProScoring.attackPriority(gain)).isCloseTo(5, within(TOLERANCE));
    assertThat(ProScoring.attackPriority(loss)).isCloseTo(-10, within(TOLERANCE));
  }

  @Test
  void attackPriorityQuintuplesAnEnemyCapital() {
    final var capital =
        new ProScoring.AttackTarget(
            1, 0, true, false, false, false, false, false, false, false, true);

    assertThat(ProScoring.attackPriority(capital)).isCloseTo(5, within(TOLERANCE));
  }

  @Test
  void attackPriorityCutsANeutralTargetToATenth() {
    final var neutral =
        new ProScoring.AttackTarget(
            10, 0, true, true, false, false, false, false, false, false, false);

    assertThat(ProScoring.attackPriority(neutral)).isCloseTo(1, within(TOLERANCE));
  }

  @Test
  void attackPriorityHalvesTheTerritoryValueOfAnAmphibiousLanding() {
    final var amphib =
        new ProScoring.AttackTarget(
            0, 3, true, false, false, true, false, false, false, false, false);

    assertThat(ProScoring.attackPriority(amphib)).isCloseTo(3, within(TOLERANCE));
  }

  @Test
  void attackedTerritoryValueCreditsHoldingWithFactoryAndCapturedUnits() {
    assertThat(ProScoring.attackedTerritoryValue(true, true, false, true, false, 2, 3))
        .isCloseTo((1 + 1 + 1 + (1 + 1.5 + 1)) * 3, within(TOLERANCE));
  }

  @Test
  void attackedTerritoryValueDocksAnAmphibiousLandingThatCantHold() {
    assertThat(ProScoring.attackedTerritoryValue(true, false, true, false, false, 0, 3))
        .isCloseTo(3, within(TOLERANCE));
  }

  @Test
  void attackValueWeighsTerritoryByOddsAndSubtractsTwoThirdsOfTheCounter() {
    assertThat(ProScoring.attackValue(4, false, 10, 50, 3))
        .isCloseTo(1 + 4 + 5 - 2, within(TOLERANCE));
  }

  @Test
  void aNeutralTerritoryThatCantBeHeldIsNotWorthAttacking() {
    assertThat(ProScoring.isNotWorthAttacking(true, false, 100, false, 1)).isTrue();
  }

  @Test
  void aNegativeValueEnemyAttackIsNotWorthIt() {
    assertThat(ProScoring.isNotWorthAttacking(false, true, -1, false, 1)).isTrue();
  }

  @Test
  void aNegativeValueNeutralAttackStandsWhenItsUnitsHaveNothingElseAndItEndsQuickly() {
    assertThat(ProScoring.isNotWorthAttacking(true, true, -1, false, 3)).isFalse();
  }

  @Test
  void aNegativeValueNeutralAttackFallsWhenItsUnitsCouldGoElsewhereOrItDragsOn() {
    assertThat(ProScoring.isNotWorthAttacking(true, true, -1, true, 3)).isTrue();
    assertThat(ProScoring.isNotWorthAttacking(true, true, -1, false, 4)).isTrue();
  }

  @Test
  void aPositiveValueAttackOnTheEnemyIsWorthIt() {
    assertThat(ProScoring.isNotWorthAttacking(false, false, 1, false, 9)).isFalse();
  }

  private static ProBattleResult attackerResult(
      final double winPercentage, final double tuvSwing, final boolean landUnitRemains) {
    return new ProBattleResult(winPercentage, tuvSwing, landUnitRemains, List.of(), List.of(), 1);
  }

  @Test
  void aTerritoryIsHeldWhenNoAttackerLandUnitSurvivesAtNoTuvLoss() {
    assertThat(ProScoring.isHeld(attackerResult(99, 0, false), false, 90)).isTrue();
  }

  @Test
  void aTerritoryIsNotHeldWhenAnAttackerLandUnitSurvives() {
    assertThat(ProScoring.isHeld(attackerResult(1, -5, true), false, 90)).isFalse();
  }

  @Test
  void aTerritoryIsNotHeldAtATuvLossEvenIfNothingCapturesIt() {
    assertThat(ProScoring.isHeld(attackerResult(1, 1, false), false, 90)).isFalse();
  }

  @Test
  void theCapitalIsHeldOnlyWhenTheAttackerRarelyWins() {
    assertThat(ProScoring.isHeld(attackerResult(9, 0, true), true, 90)).isTrue();
    assertThat(ProScoring.isHeld(attackerResult(10, 0, false), true, 90)).isFalse();
  }

  @Test
  void lowestScoringPicksTheCandidateWithTheLowestScore() {
    final var lowest = ProScoring.lowestScoring(List.of(3.0, 1.0, 2.0), score -> score);

    assertThat(lowest).contains(new ProScoring.Scored<>(1.0, 1.0));
  }

  @Test
  void lowestScoringKeepsTheFirstCandidateOnATie() {
    final var lowest = ProScoring.lowestScoring(List.of("first", "second"), candidate -> 1);

    assertThat(lowest.map(ProScoring.Scored::item)).contains("first");
  }

  @Test
  void lowestScoringFindsNothingAmongNoCandidates() {
    assertThat(ProScoring.lowestScoring(List.<String>of(), candidate -> 1)).isEmpty();
  }

  @Test
  void lowestScoringNeverPicksACandidateRuledOutWithInfinity() {
    final var lowest =
        ProScoring.lowestScoring(List.of("out"), candidate -> Double.POSITIVE_INFINITY);

    assertThat(lowest).isEmpty();
  }

  @Test
  void aTransportThatArrivesThisTurnIsZeroTurnsAway() {
    assertThat(ProScoring.turnsAway(2, 2)).isZero();
    assertThat(ProScoring.turnsAway(1, 2)).isZero();
  }

  @Test
  void aTransportFartherThanItsMoveCountsWholeExtraTurns() {
    assertThat(ProScoring.turnsAway(3, 2)).isEqualTo(1);
    assertThat(ProScoring.turnsAway(5, 2)).isEqualTo(2);
    assertThat(ProScoring.turnsAway(6, 2)).isEqualTo(2);
  }

  @Test
  void loadValueRisesWithDistanceAndFallsWithUnitsAndProduction() {
    assertThat(ProScoring.loadValue(10, 2, 3, 4)).isCloseTo(10.3, within(TOLERANCE));
  }

  @Test
  void seaUnitValueMultipliesSeaValueAndScalesTerritoryValueDownByTransports() {
    assertThat(ProScoring.seaUnitValue(0, 5, 10_000)).isCloseTo(6, within(TOLERANCE));
    assertThat(ProScoring.seaUnitValue(1, 5, 10_000)).isCloseTo(111, within(TOLERANCE));
  }

  @Test
  void amphibUnitNeedWeighsNeighboringCapacityAboveNearbyAboveSeaBorders() {
    assertThat(ProScoring.amphibUnitNeed(3, 2, 1, false, 4)).isEqualTo(2000 + 400 + 4);
  }

  @Test
  void amphibUnitNeedIgnoresCapacityTheWaitingUnitsAlreadyFill() {
    assertThat(ProScoring.amphibUnitNeed(1, 1, 5, false, 2)).isEqualTo(2);
  }

  @Test
  void aFactoryElevenFoldsHowMuchSeaBordersCountForAmphibUnits() {
    assertThat(ProScoring.amphibUnitNeed(0, 0, 0, true, 2)).isEqualTo(22);
  }

  @Test
  void airLandingValueWeighsSeaTargetsAboveLandAboveEnemiesAboveNearby() {
    assertThat(ProScoring.airLandingValue(1, 2, 3, 4, false, true, false))
        .isCloseTo(200 + 200 + 30 + 4, within(TOLERANCE));
  }

  @Test
  void airLandingValueHalvesWhenItCantBeHeldWithoutAllies() {
    assertThat(ProScoring.airLandingValue(1, 0, 0, 0, true, true, false))
        .isCloseTo(100, within(TOLERANCE));
  }

  @Test
  void airLandingValueQuartersWhenItCantBeHeldAndHasNoFactory() {
    assertThat(ProScoring.airLandingValue(1, 0, 0, 0, true, false, false))
        .isCloseTo(50, within(TOLERANCE));
  }

  @Test
  void aMissingFactoryCostsNothingWhenTheTerritoryHoldsAlone() {
    assertThat(ProScoring.airLandingValue(1, 0, 0, 0, false, false, false))
        .isCloseTo(200, within(TOLERANCE));
  }

  @Test
  void anOwnedCarrierDoublesAirLandingValue() {
    assertThat(ProScoring.airLandingValue(1, 0, 0, 0, false, true, true))
        .isCloseTo(400, within(TOLERANCE));
  }

  @Test
  void holdValueCreditsAnEighthOfAddedUnitsOnLandAndAQuarterAtSea() {
    assertThat(ProScoring.holdValue(10, 16, false)).isCloseTo(8, within(TOLERANCE));
    assertThat(ProScoring.holdValue(10, 16, true)).isCloseTo(6, within(TOLERANCE));
  }

  @Test
  void landDefensePriorityCombinesProductionFactoryAndDefenders() {
    assertThat(ProScoring.landDefensePriority(3, false, 4, false))
        .isCloseTo(6 + 2, within(TOLERANCE));
  }

  @Test
  void aFactoryAddsToAndDoublesLandDefensePriority() {
    assertThat(ProScoring.landDefensePriority(3, true, 4, false))
        .isCloseTo((6 + 4 + 2) * 2, within(TOLERANCE));
  }

  @Test
  void theCapitalElevenFoldsLandDefensePriority() {
    assertThat(ProScoring.landDefensePriority(3, false, 4, true))
        .isCloseTo((6 + 2) * 11, within(TOLERANCE));
  }

  @Test
  void seaPurchasePriorityRisesWithTransportsAndDefenders() {
    assertThat(ProScoring.seaPurchasePriority(10, 2, 5, false))
        .isCloseTo(10 * 3.5, within(TOLERANCE));
  }

  @Test
  void aSeaZoneThatNeedsDefendersIsAQuarterAsWorthBuyingFor() {
    assertThat(ProScoring.seaPurchasePriority(10, 2, 5, true))
        .isCloseTo(10 * 3.5 / 4, within(TOLERANCE));
  }
}
