package games.strategy.triplea.delegate.power.calculator;

import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.IntegerMap;

class AvailableSupportsTest {

  @Nested
  class FilterSupport {

    @Test
    void ruleThatMatchesFilterIsCopiedToNewCalculator() throws GameParseException {
      final GameData gameData = givenGameData().build();

      final GamePlayer owner = mock(GamePlayer.class);

      final UnitType unitType = new UnitType("unit", gameData);
      final Unit unit = unitType.createTemp(1, owner).get(0);

      final UnitSupportAttachment rule = new UnitSupportAttachment("rule", unitType, gameData);
      rule.setSide("offence")
          .setFaction("enemy")
          .setPlayers(List.of(owner))
          .setUnitType(Set.of(mock(UnitType.class)))
          .setBonusType("bonus")
          .setDice("roll")
          .setNumber(1);

      final UnitSupportAttachment rule2 = new UnitSupportAttachment("rule2", unitType, gameData);
      rule2
          .setSide("offence")
          .setFaction("enemy")
          .setPlayers(List.of(owner))
          .setUnitType(Set.of(mock(UnitType.class)))
          .setBonusType("bonus2")
          .setDice("strength")
          .setNumber(1);

      final AvailableSupports tracker =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(unit), List.of(rule, rule2), BattleState.Side.OFFENSE, false));

      final AvailableSupports filtered = tracker.filter(UnitSupportAttachment::getRoll);
      assertThat(filtered.getSupportLeft(rule))
          .as("The roll rule is copied to the new calculator")
          .isEqualTo(1);
      assertThat(filtered.getSupportLeft(rule2))
          .as("The strength rule is not copied to the new calculator")
          .isEqualTo(0);
      assertThat(filtered.supportRules.keySet())
          .as("Only one bonus (the roll one) is in the new calculator")
          .hasSize(1);
    }
  }

  @Nested
  class UseSupport {

    @Test
    void supportToOneUnit() throws GameParseException {
      final GameData gameData = givenGameData().build();

      final GamePlayer owner = mock(GamePlayer.class);

      final UnitType unitType = new UnitType("unit", gameData);
      final Unit unit = unitType.createTemp(1, owner).get(0);

      final UnitType supportUnitType = new UnitType("support", gameData);
      final Unit supportUnit = supportUnitType.createTemp(1, owner).get(0);

      final UnitSupportAttachment rule =
          new UnitSupportAttachment("rule", supportUnitType, gameData);
      rule.setSide("offence")
          .setFaction("enemy")
          .setPlayers(List.of(owner))
          .setBonusType("bonus")
          .setUnitType(Set.of(unitType))
          .setBonus(1)
          .setNumber(1);

      final AvailableSupports tracker =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(rule), BattleState.Side.OFFENSE, false));

      assertThat(tracker.giveSupportToUnit(unit)).as("Support unit can give one").isEqualTo(1);

      assertThat(tracker.getSupportLeft(rule))
          .as("All the support was used for the rule")
          .isEqualTo(0);
      assertThat(tracker.getUnitsGivingSupport())
          .as("The support unit gave one support")
          .isEqualTo(Map.of(supportUnit, IntegerMap.of(Map.of(unit, 1))));
    }

    @Test
    void supportWithValueOfTwoForOneUnit() throws GameParseException {
      final GameData gameData = givenGameData().build();

      final GamePlayer owner = mock(GamePlayer.class);

      final UnitType unitType = new UnitType("unit", gameData);
      final Unit unit = unitType.createTemp(1, owner).get(0);

      final UnitType supportUnitType = new UnitType("support", gameData);
      final Unit supportUnit = supportUnitType.createTemp(1, owner).get(0);

      final UnitSupportAttachment rule =
          new UnitSupportAttachment("rule", supportUnitType, gameData);
      rule.setSide("offence")
          .setFaction("enemy")
          .setPlayers(List.of(owner))
          .setBonusType("bonus")
          .setUnitType(Set.of(unitType))
          .setBonus(2)
          .setNumber(1);

      final AvailableSupports tracker =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(rule), BattleState.Side.OFFENSE, false));

      assertThat(tracker.giveSupportToUnit(unit)).as("Support unit can give 2").isEqualTo(2);

      assertThat(tracker.getSupportLeft(rule))
          .as("All the support was used for the rule")
          .isEqualTo(0);
      assertThat(tracker.getUnitsGivingSupport())
          .as("The support unit gave one support of 2")
          .isEqualTo(Map.of(supportUnit, IntegerMap.of(Map.of(unit, 2))));
    }

    @Test
    void supportToTwoUnitsButOnlyEnoughForOne() throws GameParseException {
      final GameData gameData = givenGameData().build();

      final GamePlayer owner = mock(GamePlayer.class);

      final UnitType unitType = new UnitType("unit", gameData);
      final Unit unit = unitType.createTemp(1, owner).get(0);
      final Unit unit2 = unitType.createTemp(1, owner).get(0);

      final UnitType supportUnitType = new UnitType("support", gameData);
      final Unit supportUnit = supportUnitType.createTemp(1, owner).get(0);

      final UnitSupportAttachment rule =
          new UnitSupportAttachment("rule", supportUnitType, gameData);
      rule.setSide("offence")
          .setFaction("enemy")
          .setPlayers(List.of(owner))
          .setBonusType("bonus")
          .setUnitType(Set.of(unitType))
          .setBonus(1)
          .setNumber(1);

      final AvailableSupports tracker =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(rule), BattleState.Side.OFFENSE, false));

      // give the support to the first unit
      assertThat(tracker.giveSupportToUnit(unit)).as("Support unit can give 1").isEqualTo(1);
      // attempt to give the support to the second unit
      assertThat(tracker.giveSupportToUnit(unit2))
          .as("Support unit has no more to give")
          .isEqualTo(0);

      assertThat(tracker.getUnitsGivingSupport())
          .as("The second unit should get no support as it was all used up")
          .isEqualTo(Map.of(supportUnit, IntegerMap.of(Map.of(unit, 1))));
    }

    @Test
    void supportForTwoUnits() throws GameParseException {
      final GameData gameData = givenGameData().build();

      final GamePlayer owner = mock(GamePlayer.class);

      final UnitType unitType = new UnitType("unit", gameData);
      final Unit unit = unitType.createTemp(1, owner).get(0);
      final Unit unit2 = unitType.createTemp(1, owner).get(0);

      final UnitType supportUnitType = new UnitType("support", gameData);
      final Unit supportUnit = supportUnitType.createTemp(1, owner).get(0);

      final UnitSupportAttachment rule =
          new UnitSupportAttachment("rule", supportUnitType, gameData);
      rule.setSide("offence")
          .setFaction("enemy")
          .setPlayers(List.of(owner))
          .setBonusType("bonus")
          .setUnitType(Set.of(unitType))
          .setBonus(1)
          .setNumber(2);

      final AvailableSupports tracker =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit), List.of(rule), BattleState.Side.OFFENSE, false));

      assertThat(tracker.giveSupportToUnit(unit)).as("Support can give 1").isEqualTo(1);
      assertThat(tracker.giveSupportToUnit(unit2)).as("Support can still give 1 more").isEqualTo(1);

      assertThat(tracker.getSupportLeft(rule))
          .as("All the support was used for the rule")
          .isEqualTo(0);
      assertThat(tracker.getUnitsGivingSupport())
          .as("The support unit gave one support to both")
          .isEqualTo(Map.of(supportUnit, IntegerMap.of(Map.of(unit, 1, unit2, 1))));
    }

    @Test
    void twoSupportersGiveSupportToTwoUnits() throws GameParseException {
      final GameData gameData = givenGameData().build();

      final GamePlayer owner = mock(GamePlayer.class);

      final UnitType unitType = new UnitType("unit", gameData);
      final Unit unit = unitType.createTemp(1, owner).get(0);
      final Unit unit2 = unitType.createTemp(1, owner).get(0);

      final UnitType supportUnitType = new UnitType("support", gameData);
      final Unit supportUnit = supportUnitType.createTemp(1, owner).get(0);
      final Unit supportUnit2 = supportUnitType.createTemp(1, owner).get(0);

      final UnitSupportAttachment rule =
          new UnitSupportAttachment("rule", supportUnitType, gameData);
      rule.setSide("offence")
          .setFaction("enemy")
          .setPlayers(List.of(owner))
          .setBonusType("bonus")
          .setUnitType(Set.of(unitType))
          .setBonus(1)
          .setNumber(1);

      final AvailableSupports tracker =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit, supportUnit2),
                  List.of(rule),
                  BattleState.Side.OFFENSE,
                  false));

      assertThat(tracker.giveSupportToUnit(unit)).as("First support unit can support").isEqualTo(1);
      assertThat(tracker.giveSupportToUnit(unit2))
          .as("Second support unit can support")
          .isEqualTo(1);

      assertThat(tracker.getSupportLeft(rule))
          .as("All the support was used for the rule")
          .isEqualTo(0);
      assertThat(tracker.getUnitsGivingSupport())
          .as(
              "The first support unit supports the first unit and "
                  + "the second support unit supports the second unit")
          .isEqualTo(
              Map.of(
                  supportUnit,
                  IntegerMap.of(Map.of(unit, 1)),
                  supportUnit2,
                  IntegerMap.of(Map.of(unit2, 1))));
    }

    @Test
    void twoSupportersInAStackGiveSupportToOneUnit() throws GameParseException {
      final GameData gameData = givenGameData().build();

      final GamePlayer owner = mock(GamePlayer.class);

      final UnitType unitType = new UnitType("unit", gameData);
      final Unit unit = unitType.createTemp(1, owner).get(0);
      final Unit unit2 = unitType.createTemp(1, owner).get(0);

      final UnitType supportUnitType = new UnitType("support", gameData);
      final Unit supportUnit = supportUnitType.createTemp(1, owner).get(0);
      final Unit supportUnit2 = supportUnitType.createTemp(1, owner).get(0);

      final UnitSupportAttachment rule =
          new UnitSupportAttachment("rule", supportUnitType, gameData);
      rule.setSide("offence")
          .setFaction("enemy")
          .setPlayers(List.of(owner))
          // allow this support to stack up to 2 times
          .setBonusType("2:bonus")
          .setUnitType(Set.of(unitType))
          .setBonus(1)
          .setNumber(1);

      final AvailableSupports tracker =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit, supportUnit2),
                  List.of(rule),
                  BattleState.Side.OFFENSE,
                  false));

      assertThat(tracker.giveSupportToUnit(unit))
          .as("All support is given because of stacking")
          .isEqualTo(2);
      assertThat(tracker.giveSupportToUnit(unit2))
          .as("No support left because of stacking")
          .isEqualTo(0);

      assertThat(tracker.getSupportLeft(rule))
          .as("All the support was used for the rule")
          .isEqualTo(0);
      assertThat(tracker.getUnitsGivingSupport())
          .as("The first unit gets all the support because of stack of 2")
          .isEqualTo(
              Map.of(
                  supportUnit,
                  IntegerMap.of(Map.of(unit, 1)),
                  supportUnit2,
                  IntegerMap.of(Map.of(unit, 1))));
    }

    @Test
    void twoSupportersInAStackWithSupportNumberOfTwoGiveSupportToTwoUnits()
        throws GameParseException {
      final GameData gameData = givenGameData().build();

      final GamePlayer owner = mock(GamePlayer.class);

      final UnitType unitType = new UnitType("unit", gameData);
      final Unit unit = unitType.createTemp(1, owner).get(0);
      final Unit unit2 = unitType.createTemp(1, owner).get(0);

      final UnitType supportUnitType = new UnitType("support", gameData);
      final Unit supportUnit = supportUnitType.createTemp(1, owner).get(0);
      final Unit supportUnit2 = supportUnitType.createTemp(1, owner).get(0);

      final UnitSupportAttachment rule =
          new UnitSupportAttachment("rule", supportUnitType, gameData);
      rule.setSide("offence")
          .setFaction("enemy")
          .setPlayers(List.of(owner))
          // allow this support to stack up to 2 times
          .setBonusType("2:bonus")
          .setUnitType(Set.of(unitType))
          .setBonus(1)
          .setNumber(2);

      final AvailableSupports tracker =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit, supportUnit2),
                  List.of(rule),
                  BattleState.Side.OFFENSE,
                  false));

      assertThat(tracker.giveSupportToUnit(unit))
          .as("First unit gets one support from each of the two distinct supporters")
          .isEqualTo(2);
      assertThat(tracker.giveSupportToUnit(unit2))
          .as("Second unit also gets one support from each of the two distinct supporters")
          .isEqualTo(2);

      assertThat(tracker.getSupportLeft(rule))
          .as("All the support was used for the rule")
          .isEqualTo(0);
      assertThat(tracker.getUnitsGivingSupport())
          .as(
              "Support is spread across targets: a single supporter contributes at most one "
                  + "support per target unit, so each supporter gives 1 to unit and 1 to unit2 "
                  + "rather than dumping its full number on one target.")
          .isEqualTo(
              Map.of(
                  supportUnit,
                  IntegerMap.of(Map.of(unit, 1, unit2, 1)),
                  supportUnit2,
                  IntegerMap.of(Map.of(unit, 1, unit2, 1))));
    }

    @Test
    void singleSupporterCannotStackOnOneUnitWhenOtherTargetsExist() throws GameParseException {
      // The exact scenario from issue #12593: one artillery (number=3) with two infantry,
      // and a bonus type that allows up to 3 supports per target. Previously the artillery
      // dumped all 3 supports on the first infantry; now it spreads one to each.
      final GameData gameData = givenGameData().build();

      final GamePlayer owner = mock(GamePlayer.class);

      final UnitType unitType = new UnitType("infantry", gameData);
      final Unit infantry1 = unitType.createTemp(1, owner).get(0);
      final Unit infantry2 = unitType.createTemp(1, owner).get(0);

      final UnitType supportUnitType = new UnitType("artillery", gameData);
      final Unit artillery = supportUnitType.createTemp(1, owner).get(0);

      final UnitSupportAttachment rule =
          new UnitSupportAttachment("rule", supportUnitType, gameData);
      rule.setSide("offence")
          .setFaction("enemy")
          .setPlayers(List.of(owner))
          // bonus stacks up to 3 on one target
          .setBonusType("3:bonus")
          .setUnitType(Set.of(unitType))
          .setBonus(1)
          .setNumber(3);

      final AvailableSupports tracker =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(artillery), List.of(rule), BattleState.Side.OFFENSE, false));

      assertThat(tracker.giveSupportToUnit(infantry1))
          .as("Artillery gives one support to the first infantry")
          .isEqualTo(1);
      assertThat(tracker.giveSupportToUnit(infantry2))
          .as("Artillery gives one support to the second infantry")
          .isEqualTo(1);

      assertThat(tracker.getUnitsGivingSupport())
          .as(
              "Artillery cannot stack a second support onto the same infantry, even though stack "
                  + "count and remaining number both allow it.")
          .isEqualTo(Map.of(artillery, IntegerMap.of(Map.of(infantry1, 1, infantry2, 1))));
    }

    @Test
    void singleSupporterGivesOneToLoneTarget() throws GameParseException {
      // Edge case for the new behavior: one supporter with one target gives exactly one
      // support, even when its number and the bonus stack would allow more. Stacking on the
      // same target requires multiple distinct supporters.
      final GameData gameData = givenGameData().build();

      final GamePlayer owner = mock(GamePlayer.class);

      final UnitType unitType = new UnitType("infantry", gameData);
      final Unit infantry = unitType.createTemp(1, owner).get(0);

      final UnitType supportUnitType = new UnitType("artillery", gameData);
      final Unit artillery = supportUnitType.createTemp(1, owner).get(0);

      final UnitSupportAttachment rule =
          new UnitSupportAttachment("rule", supportUnitType, gameData);
      rule.setSide("offence")
          .setFaction("enemy")
          .setPlayers(List.of(owner))
          .setBonusType("3:bonus")
          .setUnitType(Set.of(unitType))
          .setBonus(1)
          .setNumber(3);

      final AvailableSupports tracker =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(artillery), List.of(rule), BattleState.Side.OFFENSE, false));

      assertThat(tracker.giveSupportToUnit(infantry))
          .as("Single supporter contributes exactly one support to the lone target")
          .isEqualTo(1);
      assertThat(tracker.getUnitsGivingSupport())
          .isEqualTo(Map.of(artillery, IntegerMap.of(Map.of(infantry, 1))));
    }

    @Test
    void threeSupportersStackingOnOneUnitRespectsBonusCount() throws GameParseException {
      // 3 supporters, each number=1, stack count=2. Only 2 of the 3 supporters should
      // contribute to a single target (capped by the stack count).
      final GameData gameData = givenGameData().build();

      final GamePlayer owner = mock(GamePlayer.class);

      final UnitType unitType = new UnitType("unit", gameData);
      final Unit unit = unitType.createTemp(1, owner).get(0);

      final UnitType supportUnitType = new UnitType("support", gameData);
      final Unit s1 = supportUnitType.createTemp(1, owner).get(0);
      final Unit s2 = supportUnitType.createTemp(1, owner).get(0);
      final Unit s3 = supportUnitType.createTemp(1, owner).get(0);

      final UnitSupportAttachment rule =
          new UnitSupportAttachment("rule", supportUnitType, gameData);
      rule.setSide("offence")
          .setFaction("enemy")
          .setPlayers(List.of(owner))
          .setBonusType("2:bonus")
          .setUnitType(Set.of(unitType))
          .setBonus(1)
          .setNumber(1);

      final AvailableSupports tracker =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(s1, s2, s3), List.of(rule), BattleState.Side.OFFENSE, false));

      assertThat(tracker.getSupportLeft(rule))
          .as("Three supporters, each with 1 support")
          .isEqualTo(3);
      assertThat(tracker.giveSupportToUnit(unit))
          .as("Only 2 of 3 supporters contribute (stack count cap)")
          .isEqualTo(2);
      assertThat(tracker.getSupportLeft(rule))
          .as("One supporter remains with capacity")
          .isEqualTo(1);
    }

    @Test
    void twoRulesOfDifferentBonusAlwaysStack() throws GameParseException {
      final GameData gameData = givenGameData().build();

      final GamePlayer owner = mock(GamePlayer.class);

      final UnitType unitType = new UnitType("unit", gameData);
      final Unit unit = unitType.createTemp(1, owner).get(0);

      final UnitType supportUnitType = new UnitType("support", gameData);
      final Unit supportUnit = supportUnitType.createTemp(1, owner).get(0);

      final UnitSupportAttachment rule =
          new UnitSupportAttachment("rule", supportUnitType, gameData);
      rule.setSide("offence")
          .setFaction("enemy")
          .setPlayers(List.of(owner))
          .setBonusType("bonus")
          .setUnitType(Set.of(unitType))
          .setBonus(1)
          .setNumber(1);

      final UnitType supportUnitType2 = new UnitType("support2", gameData);
      final Unit supportUnit2 = supportUnitType2.createTemp(1, owner).get(0);

      final UnitSupportAttachment rule2 =
          new UnitSupportAttachment("rule2", supportUnitType2, gameData);
      rule2
          .setSide("offence")
          .setFaction("enemy")
          .setPlayers(List.of(owner))
          .setBonusType("bonus2")
          .setUnitType(Set.of(unitType))
          .setBonus(1)
          .setNumber(1);

      final AvailableSupports tracker =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit, supportUnit2),
                  List.of(rule, rule2),
                  BattleState.Side.OFFENSE,
                  false));

      assertThat(tracker.giveSupportToUnit(unit))
          .as("Both support units gave support")
          .isEqualTo(2);

      assertThat(tracker.getUnitsGivingSupport())
          .as("Both support units gave their support")
          .isEqualTo(
              Map.of(
                  supportUnit,
                  IntegerMap.of(Map.of(unit, 1)),
                  supportUnit2,
                  IntegerMap.of(Map.of(unit, 1))));
      assertThat(tracker.getSupportLeft(rule))
          .as("First rule should have no support left")
          .isEqualTo(0);
      assertThat(tracker.getSupportLeft(rule2))
          .as("Second rule should have no support left")
          .isEqualTo(0);
    }

    @Test
    void twoRulesOfSameBonusWithStackOf1() throws GameParseException {
      final GameData gameData = givenGameData().build();

      final GamePlayer owner = mock(GamePlayer.class);

      final UnitType unitType = new UnitType("unit", gameData);
      final Unit unit = unitType.createTemp(1, owner).get(0);

      final UnitType supportUnitType = new UnitType("support", gameData);
      final Unit supportUnit = supportUnitType.createTemp(1, owner).get(0);

      final UnitSupportAttachment rule =
          new UnitSupportAttachment("rule", supportUnitType, gameData);
      rule.setSide("offence")
          .setFaction("enemy")
          .setPlayers(List.of(owner))
          .setBonusType("bonus")
          .setUnitType(Set.of(unitType))
          .setBonus(1)
          .setNumber(1);

      final UnitType supportUnitType2 = new UnitType("support2", gameData);
      final Unit supportUnit2 = supportUnitType2.createTemp(1, owner).get(0);

      final UnitSupportAttachment rule2 =
          new UnitSupportAttachment("rule2", supportUnitType2, gameData);
      rule2
          .setSide("offence")
          .setFaction("enemy")
          .setPlayers(List.of(owner))
          .setBonusType("bonus")
          .setUnitType(Set.of(unitType))
          .setBonus(1)
          .setNumber(1);

      final AvailableSupports tracker =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit, supportUnit2),
                  List.of(rule, rule2),
                  BattleState.Side.OFFENSE,
                  false));

      assertThat(tracker.giveSupportToUnit(unit))
          .as("Only the first rule can give its support")
          .isEqualTo(1);

      assertThat(tracker.getUnitsGivingSupport())
          .as("Only the first rule gives support because the stack size is 1")
          .isEqualTo(Map.of(supportUnit, IntegerMap.of(Map.of(unit, 1))));
      assertThat(tracker.getSupportLeft(rule))
          .as("First rule should have no support left")
          .isEqualTo(0);
      assertThat(tracker.getSupportLeft(rule2))
          .as("Second rule should have support left")
          .isEqualTo(1);
    }

    @Test
    void twoRulesOfSameBonusWithStackOf2() throws GameParseException {
      final GameData gameData = givenGameData().build();

      final GamePlayer owner = mock(GamePlayer.class);

      final UnitType unitType = new UnitType("unit", gameData);
      final Unit unit = unitType.createTemp(1, owner).get(0);

      final UnitType supportUnitType = new UnitType("support", gameData);
      final Unit supportUnit = supportUnitType.createTemp(1, owner).get(0);

      final UnitSupportAttachment rule =
          new UnitSupportAttachment("rule", supportUnitType, gameData);
      rule.setSide("offence")
          .setFaction("enemy")
          .setPlayers(List.of(owner))
          .setBonusType("2:bonus")
          .setUnitType(Set.of(unitType))
          .setBonus(1)
          .setNumber(1);

      final UnitType supportUnitType2 = new UnitType("support2", gameData);
      final Unit supportUnit2 = supportUnitType2.createTemp(1, owner).get(0);

      final UnitSupportAttachment rule2 =
          new UnitSupportAttachment("rule2", supportUnitType2, gameData);
      rule2
          .setSide("offence")
          .setFaction("enemy")
          .setPlayers(List.of(owner))
          .setBonusType("2:bonus")
          .setUnitType(Set.of(unitType))
          .setBonus(1)
          .setNumber(1);

      final AvailableSupports tracker =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit, supportUnit2),
                  List.of(rule, rule2),
                  BattleState.Side.OFFENSE,
                  false));

      assertThat(tracker.giveSupportToUnit(unit)).as("All support can be given").isEqualTo(2);

      assertThat(tracker.getUnitsGivingSupport())
          .as("Both support units gave their support")
          .isEqualTo(
              Map.of(
                  supportUnit,
                  IntegerMap.of(Map.of(unit, 1)),
                  supportUnit2,
                  IntegerMap.of(Map.of(unit, 1))));
      assertThat(tracker.getSupportLeft(rule))
          .as("First rule should have no support left")
          .isEqualTo(0);
      assertThat(tracker.getSupportLeft(rule2))
          .as("Second rule should have no support left")
          .isEqualTo(0);
    }
  }
}
