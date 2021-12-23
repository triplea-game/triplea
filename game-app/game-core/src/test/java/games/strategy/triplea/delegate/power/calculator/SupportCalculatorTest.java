package games.strategy.triplea.delegate.power.calculator;

import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SupportCalculatorTest {

  @Test
  void ruleIsAddedToTheSupportCalculator() throws GameParseException {
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
        .setNumber(1);

    final SupportCalculator tracker =
        new SupportCalculator(List.of(unit), List.of(rule), BattleState.Side.OFFENSE, false);
    assertThat("There is only one bonus type", tracker.getSupportRules().size(), is(1));
    assertThat("The rule only has one support available", tracker.getSupport(rule), is(1));
  }

  @Test
  void ruleIsIgnoredIfNoPlayers() throws GameParseException {
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
        .setNumber(1);

    final UnitSupportAttachment rule2 = new UnitSupportAttachment("rule2", unitType, gameData);
    rule2
        .setSide("offence")
        .setFaction("enemy")
        .setPlayers(List.of())
        .setUnitType(Set.of(mock(UnitType.class)))
        .setBonusType("bonus2")
        .setNumber(1);

    final SupportCalculator tracker =
        new SupportCalculator(List.of(unit), List.of(rule, rule2), BattleState.Side.OFFENSE, false);
    assertThat("Rule with players is added", tracker.getSupport(rule), is(1));
    assertThat("Rule without players is not added", tracker.getSupport(rule2), is(0));
  }

  @Test
  void ruleIsIgnoredIfNullUnitTypes() throws GameParseException {
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
        .setNumber(1);

    final UnitSupportAttachment rule2 = new UnitSupportAttachment("rule2", unitType, gameData);
    rule2
        .setSide("offence")
        .setFaction("enemy")
        .setPlayers(List.of(owner))
        .setUnitType(null)
        .setBonusType("bonus2")
        .setNumber(1);

    final SupportCalculator tracker =
        new SupportCalculator(List.of(unit), List.of(rule, rule2), BattleState.Side.OFFENSE, false);
    assertThat("Rule with unit types is added", tracker.getSupport(rule), is(1));
    assertThat("Rule without unit types is not added", tracker.getSupport(rule2), is(0));
  }

  @Test
  void ruleIsIgnoredIfEmptyUnitTypes() throws GameParseException {
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
        .setNumber(1);

    final UnitSupportAttachment rule2 = new UnitSupportAttachment("rule2", unitType, gameData);
    rule2
        .setSide("offence")
        .setFaction("enemy")
        .setPlayers(List.of(owner))
        .setUnitType(Set.of())
        .setBonusType("bonus2")
        .setNumber(1);

    final SupportCalculator tracker =
        new SupportCalculator(List.of(unit), List.of(rule, rule2), BattleState.Side.OFFENSE, false);
    assertThat("Rule with unit types is added", tracker.getSupport(rule), is(1));
    assertThat("Rule without unit types is not added", tracker.getSupport(rule2), is(0));
  }

  @Test
  void defenceRuleIsIgnoredIfOffenceIsWanted() throws GameParseException {
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
        .setNumber(1);

    final UnitSupportAttachment rule2 = new UnitSupportAttachment("rule2", unitType, gameData);
    rule2
        .setSide("defence")
        .setFaction("enemy")
        .setPlayers(List.of(owner))
        .setUnitType(Set.of(mock(UnitType.class)))
        .setBonusType("bonus2")
        .setNumber(1);

    final SupportCalculator tracker =
        new SupportCalculator(List.of(unit), List.of(rule, rule2), BattleState.Side.OFFENSE, false);
    assertThat("Offence rule has support", tracker.getSupport(rule), is(1));
    assertThat("Defence rule is ignored", tracker.getSupport(rule2), is(0));
  }

  @Test
  void offenceRuleIsIgnoredIfDefenceIsWanted() throws GameParseException {
    final GameData gameData = givenGameData().build();

    final GamePlayer owner = mock(GamePlayer.class);

    final UnitType unitType = new UnitType("unit", gameData);
    final Unit unit = unitType.createTemp(1, owner).get(0);

    final UnitSupportAttachment rule = new UnitSupportAttachment("rule", unitType, gameData);
    rule.setSide("defence")
        .setFaction("enemy")
        .setPlayers(List.of(owner))
        .setUnitType(Set.of(mock(UnitType.class)))
        .setBonusType("bonus")
        .setNumber(1);

    final UnitSupportAttachment rule2 = new UnitSupportAttachment("rule2", unitType, gameData);
    rule2
        .setSide("offence")
        .setFaction("enemy")
        .setPlayers(List.of(owner))
        .setUnitType(Set.of(mock(UnitType.class)))
        .setBonusType("bonus2")
        .setNumber(1);

    final SupportCalculator tracker =
        new SupportCalculator(List.of(unit), List.of(rule, rule2), BattleState.Side.DEFENSE, false);
    assertThat("Defence rule has support", tracker.getSupport(rule), is(1));
    assertThat("Offence rule is ignored", tracker.getSupport(rule2), is(0));
  }

  @Test
  void alliedRuleIsIgnoredIfEnemyIsWanted() throws GameParseException {
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
        .setNumber(1);

    final UnitSupportAttachment rule2 = new UnitSupportAttachment("rule2", unitType, gameData);
    rule2
        .setSide("offence")
        .setFaction("allied")
        .setPlayers(List.of(owner))
        .setUnitType(Set.of(mock(UnitType.class)))
        .setBonusType("bonus2")
        .setNumber(1);

    final SupportCalculator tracker =
        new SupportCalculator(List.of(unit), List.of(rule, rule2), BattleState.Side.OFFENSE, false);
    assertThat("Enemy rule has support", tracker.getSupport(rule), is(1));
    assertThat("Allied rule is ignored", tracker.getSupport(rule2), is(0));
  }

  @Test
  void enemyRuleIsIgnoredIfAlliedIsWanted() throws GameParseException {
    final GameData gameData = givenGameData().build();

    final GamePlayer owner = mock(GamePlayer.class);

    final UnitType unitType = new UnitType("unit", gameData);
    final Unit unit = unitType.createTemp(1, owner).get(0);

    final UnitSupportAttachment rule = new UnitSupportAttachment("rule", unitType, gameData);
    rule.setSide("offence")
        .setFaction("allied")
        .setPlayers(List.of(owner))
        .setUnitType(Set.of(mock(UnitType.class)))
        .setBonusType("bonus")
        .setNumber(1);

    final UnitSupportAttachment rule2 = new UnitSupportAttachment("rule2", unitType, gameData);
    rule2
        .setSide("offence")
        .setFaction("enemy")
        .setPlayers(List.of(owner))
        .setUnitType(Set.of(mock(UnitType.class)))
        .setBonusType("bonus2")
        .setNumber(1);

    final SupportCalculator tracker =
        new SupportCalculator(List.of(unit), List.of(rule, rule2), BattleState.Side.OFFENSE, true);
    assertThat("Allied rule has support", tracker.getSupport(rule), is(1));
    assertThat("Enemy rule is ignored", tracker.getSupport(rule2), is(0));
  }

  @Test
  void ruleWithNoMatchingSupportersIsIgnored() throws GameParseException {
    final GameData gameData = givenGameData().build();

    final GamePlayer owner = mock(GamePlayer.class);

    final UnitType unitType = new UnitType("unit", gameData);
    final Unit unit = unitType.createTemp(1, owner).get(0);

    final UnitSupportAttachment rule = new UnitSupportAttachment("rule", unitType, gameData);
    rule.setSide("offence")
        .setFaction("allied")
        .setPlayers(List.of(owner))
        .setUnitType(Set.of(mock(UnitType.class)))
        .setBonusType("bonus")
        .setNumber(1);

    final UnitType unusedUnitType = new UnitType("unit2", gameData);
    final UnitSupportAttachment rule2 =
        new UnitSupportAttachment("rule2", unusedUnitType, gameData);
    rule2
        .setSide("offence")
        .setFaction("allied")
        .setPlayers(List.of(owner))
        .setUnitType(Set.of(mock(UnitType.class)))
        .setBonusType("bonus2")
        .setNumber(1);

    final SupportCalculator tracker =
        new SupportCalculator(List.of(unit), List.of(rule, rule2), BattleState.Side.OFFENSE, true);
    assertThat("Rule with a supporter is added", tracker.getSupport(rule), is(1));
    assertThat("Rule without a supporter is ignored", tracker.getSupport(rule2), is(0));
  }

  @Test
  void improvedArtilleryTechnologyDoublesTheSupport() throws GameParseException {
    final GameData gameData = givenGameData().build();

    final GamePlayer owner = mock(GamePlayer.class);
    final TechAttachment techAttachment = mock(TechAttachment.class);
    when(owner.getTechAttachment()).thenReturn(techAttachment);
    when(techAttachment.getImprovedArtillerySupport()).thenReturn(true);

    final UnitType unitType = new UnitType("unit", gameData);
    final Unit unit = unitType.createTemp(1, owner).get(0);

    final UnitSupportAttachment rule = new UnitSupportAttachment("rule", unitType, gameData);
    rule.setSide("offence")
        .setFaction("allied")
        .setPlayers(List.of(owner))
        .setUnitType(Set.of(mock(UnitType.class)))
        .setImpArtTech(true)
        .setBonusType("bonus")
        .setNumber(1);

    final SupportCalculator tracker =
        new SupportCalculator(List.of(unit), List.of(rule), BattleState.Side.OFFENSE, true);
    assertThat(
        "Rule for improved artillery gives double the support "
            + "when the unit has improved artillery",
        tracker.getSupport(rule),
        is(2));
  }

  @Test
  void ruleMustWantImprovedArtilleryTechnologyToGetTheBonus() throws GameParseException {
    final GameData gameData = givenGameData().build();

    final GamePlayer owner = mock(GamePlayer.class);
    final TechAttachment techAttachment = mock(TechAttachment.class);
    when(owner.getTechAttachment()).thenReturn(techAttachment);
    when(techAttachment.getImprovedArtillerySupport()).thenReturn(true);

    final UnitType unitType = new UnitType("unit", gameData);
    final Unit unit = unitType.createTemp(1, owner).get(0);

    final UnitSupportAttachment rule = new UnitSupportAttachment("rule", unitType, gameData);
    rule.setSide("offence")
        .setFaction("allied")
        .setPlayers(List.of(owner))
        .setUnitType(Set.of(mock(UnitType.class)))
        .setImpArtTech(false)
        .setBonusType("bonus")
        .setNumber(1);

    final SupportCalculator tracker =
        new SupportCalculator(List.of(unit), List.of(rule), BattleState.Side.OFFENSE, true);
    assertThat(
        "Rule that doesn't use improved artillery doesn't give double the support "
            + "when the unit has improved artillery",
        tracker.getSupport(rule),
        is(1));
  }

  @Test
  void unitWithoutImprovedArtilleryDoesNotGetBonusWhenRuleUsesTech() throws GameParseException {
    final GameData gameData = givenGameData().build();

    final GamePlayer owner = mock(GamePlayer.class);
    final TechAttachment techAttachment = mock(TechAttachment.class);
    when(owner.getTechAttachment()).thenReturn(techAttachment);
    when(techAttachment.getImprovedArtillerySupport()).thenReturn(true);
    final GamePlayer ownerWithoutImprovedTechnology = mock(GamePlayer.class);

    final UnitType unitType = new UnitType("unit", gameData);
    final Unit unit = unitType.createTemp(1, owner).get(0);
    final Unit unitWithoutImprovedTechnology =
        unitType.createTemp(1, ownerWithoutImprovedTechnology).get(0);

    final UnitSupportAttachment rule = new UnitSupportAttachment("rule", unitType, gameData);
    rule.setSide("offence")
        .setFaction("allied")
        .setPlayers(List.of(owner, ownerWithoutImprovedTechnology))
        .setUnitType(Set.of(mock(UnitType.class)))
        .setImpArtTech(true)
        .setBonusType("bonus")
        .setNumber(1);

    final SupportCalculator tracker =
        new SupportCalculator(
            List.of(unit, unitWithoutImprovedTechnology),
            List.of(rule),
            BattleState.Side.OFFENSE,
            true);
    assertThat(
        "Unit with improved technology has a value of 2 while the unit without has a value of 1",
        tracker.getSupport(rule),
        is(3));
  }
}
