package games.strategy.triplea.ai.pro.data;

import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.artillery;
import static games.strategy.triplea.delegate.GameDataTestUtil.battleship;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProPurchaseOptionTest {
  private final GameData data = TestMapGameData.WW2V3_1941.getGameData();
  private final GamePlayer russia = russians(data);
  private final List<Unit> localInfantry = infantry(data).createTemp(1, russia);

  private ProPurchaseOption option(final String ruleName, final UnitType unitType) {
    return new ProPurchaseOption(
        data.getProductionRuleList().getProductionRule(ruleName), unitType, russia, data);
  }

  @Test
  void defenseEfficiencyWithoutAttackSupportDropsArtillerySupportCredit() {
    final ProPurchaseOption artillery = option("buyArtillery", artillery(data));

    assertThat(
            artillery.getDefenseEfficiencyWithoutAttackSupport(1, data, localInfantry, List.of()))
        .isLessThan(artillery.getDefenseEfficiency(1, data, localInfantry, List.of()));
  }

  @Test
  void defenseEfficiencyWithoutAttackSupportKeepsUnitsThatGiveNoAttackSupport() {
    final ProPurchaseOption infantry = option("buyInfantry", infantry(data));

    assertThat(infantry.getDefenseEfficiencyWithoutAttackSupport(1, data, localInfantry, List.of()))
        .isCloseTo(infantry.getDefenseEfficiency(1, data, localInfantry, List.of()), within(1e-9));
  }

  @Test
  void attackAndDefenseCountEveryRoll() {
    final UnitType infantry = infantry(data);
    final ProPurchaseOption oneRoll = option("buyInfantry", infantry);
    infantry.getUnitAttachment().setAttackRolls(2);
    infantry.getUnitAttachment().setDefenseRolls(3);
    final ProPurchaseOption manyRolls = option("buyInfantry", infantry);

    assertThat(manyRolls.getAttack()).isCloseTo(oneRoll.getAttack() * 2, within(1e-9));
    assertThat(manyRolls.getDefense()).isCloseTo(oneRoll.getDefense() * 3, within(1e-9));
  }

  @Test
  void suicideOnAttackUnitIsSingleUseAndRanksBelowReusableTwin() {
    final ProPurchaseOption before = option("buyInfantry", infantry(data));
    infantry(data).getUnitAttachment().setIsSuicideOnAttack(true);

    final ProPurchaseOption after = option("buyInfantry", infantry(data));

    assertThat(before.isSingleUse()).isFalse();
    assertThat(after.isSingleUse()).isTrue();
    assertThat(after.getAttackEfficiency(1, data, localInfantry, List.of()))
        .isLessThan(before.getAttackEfficiency(1, data, localInfantry, List.of()));
  }

  @Test
  void hardBetaOffersSuicideUnitInTheNormalLandCategories() {
    infantry(data).getUnitAttachment().setIsSuicideOnAttack(true);
    infantry(data).getUnitAttachment().setIsSuicideOnDefense(true);

    final var options = new ProPurchaseOptionMap(russia, data, true);

    assertThat(options.getLandOptions())
        .extracting(ProPurchaseOption::getUnitType)
        .contains(infantry(data));
  }

  @Test
  void hardKeepsSuicideUnitOutOfEveryBuyCategory() {
    infantry(data).getUnitAttachment().setIsSuicideOnAttack(true);
    infantry(data).getUnitAttachment().setIsSuicideOnDefense(true);

    final var options = new ProPurchaseOptionMap(russia, data, false);

    assertThat(options.getLandOptions())
        .extracting(ProPurchaseOption::getUnitType)
        .doesNotContain(infantry(data));
    assertThat(options.getAllOptions())
        .extracting(ProPurchaseOption::getUnitType)
        .contains(infantry(data));
  }

  @Test
  void ruleWithoutPuCostHasFiniteEfficiencies() {
    final var freeRule = new ProductionRule("buyFreeInfantry", data);
    freeRule.addResult(infantry(data), 1);
    final var free = new ProPurchaseOption(freeRule, infantry(data), russia, data);

    assertThat(free.getAttackEfficiency(1, data, localInfantry, List.of())).isFinite();
    assertThat(free.getDefenseEfficiency(1, data, localInfantry, List.of())).isFinite();
    assertThat(free.getFodderEfficiency(1, data, localInfantry, List.of())).isFinite();
    assertThat(free.getAmphibEfficiency(data, localInfantry, List.of())).isFinite();
    assertThat(free.getAttackEfficiency()).isFinite();
    assertThat(free.getTransportEfficiency()).isFinite();
  }

  /** Russian infantry takes 1 off the strength of each enemy {@code target} attacking it. */
  private void givenRussianInfantryWeakensAttacking(final UnitType target)
      throws GameParseException {
    final var weakenEnemy =
        new UnitSupportAttachment(
                Constants.SUPPORT_ATTACHMENT_PREFIX + "AntiTank", infantry(data), data)
            .setSide("defence")
            .setFaction("enemy")
            .setPlayers(List.of(russia))
            .setUnitType(Set.of(target))
            .setBonusType("AntiTank")
            .setBonus(-1)
            .setDice("strength")
            .setNumber(1);
    infantry(data).addAttachment(weakenEnemy.getName(), weakenEnemy);
  }

  private ProPurchaseOption infantryOption(final boolean hardBeta) {
    return new ProPurchaseOption(
        data.getProductionRuleList().getProductionRule("buyInfantry"),
        infantry(data),
        russia,
        data,
        hardBeta);
  }

  @Test
  void hardBetaValuesWeakeningAttackingEnemyArmourAsDefense() throws GameParseException {
    givenRussianInfantryWeakensAttacking(armour(data));

    assertThat(infantryOption(true).getDefenseEfficiency(1, data, localInfantry, List.of()))
        .isGreaterThan(
            infantryOption(false).getDefenseEfficiency(1, data, localInfantry, List.of()));
  }

  @Test
  void hardBetaGivesNoCreditForWeakeningUnitsOutsideTheBuyersDomain() throws GameParseException {
    // Battleships are outside a land unit's domain, so the rule finds no target.
    givenRussianInfantryWeakensAttacking(battleship(data));

    assertThat(infantryOption(true).getDefenseEfficiency(1, data, localInfantry, List.of()))
        .isCloseTo(
            infantryOption(false).getDefenseEfficiency(1, data, localInfantry, List.of()),
            within(1e-9));
  }
}
