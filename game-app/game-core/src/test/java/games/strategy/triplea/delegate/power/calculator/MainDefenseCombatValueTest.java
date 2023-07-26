package games.strategy.triplea.delegate.power.calculator;

import static games.strategy.triplea.Constants.TERRITORYEFFECT_ATTACHMENT_NAME;
import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.attachments.TerritoryEffectAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.IntegerMap;

class MainDefenseCombatValueTest {

  @Nested
  class MainDefenseRollTest {

    @Test
    void calculatesValue() throws GameParseException {
      final GamePlayer player = mock(GamePlayer.class);

      final GameData gameData = givenGameData().build();

      final UnitType unitType = new UnitType("test", gameData);
      final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      final Unit unit = unitType.createTemp(1, player).get(0);
      unit.getUnitAttachment().setDefenseRolls(3);

      final Unit supportUnit = unitType.createTemp(1, player).get(0);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitOffenseSupportAttachment(gameData, unitType, "test")
              .setBonus(2)
              .setPlayers(List.of(player))
              .setUnitType(Set.of(unitType));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit),
                  Set.of(unitSupportAttachment),
                  BattleState.Side.OFFENSE,
                  true));

      final Unit enemySupportUnit = unitType.createTemp(1, player).get(0);
      final UnitSupportAttachment enemyUnitSupportAttachment =
          givenUnitDefenseSupportAttachment(gameData, unitType, "test2")
              .setBonus(-1)
              .setPlayers(List.of(player))
              .setUnitType(Set.of(unitType));

      final AvailableSupports enemySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(enemySupportUnit),
                  Set.of(enemyUnitSupportAttachment),
                  BattleState.Side.DEFENSE,
                  false));

      final MainDefenseCombatValue.MainDefenseRoll roll =
          new MainDefenseCombatValue.MainDefenseRoll(friendlySupport, enemySupport);
      assertThat(
          "Roll starts at 3, friendly adds 2, enemy removes 1: total 4",
          roll.getRoll(unit).getValue(),
          is(4));
    }

    UnitSupportAttachment givenUnitOffenseSupportAttachment(
        final GameData gameData, final UnitType unitType, final String name)
        throws GameParseException {
      return new UnitSupportAttachment("rule" + name, unitType, gameData)
          .setBonus(1)
          .setBonusType("bonus" + name)
          .setDice("roll")
          .setNumber(1)
          .setSide("offence")
          .setFaction("allied");
    }

    UnitSupportAttachment givenUnitDefenseSupportAttachment(
        final GameData gameData, final UnitType unitType, final String name)
        throws GameParseException {
      return new UnitSupportAttachment("rule" + name, unitType, gameData)
          .setBonus(1)
          .setBonusType("bonus" + name)
          .setDice("roll")
          .setNumber(1)
          .setSide("defence")
          .setFaction("enemy");
    }

    @Test
    void calculatesSupportUsed() throws GameParseException {
      final GamePlayer player = mock(GamePlayer.class);

      final GameData gameData = givenGameData().build();

      final UnitType unitType = new UnitType("test", gameData);
      final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      final Unit unit = unitType.createTemp(1, player).get(0);
      unit.getUnitAttachment().setDefenseRolls(3);

      final Unit supportUnit = unitType.createTemp(1, player).get(0);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitOffenseSupportAttachment(gameData, unitType, "test")
              .setBonus(2)
              .setPlayers(List.of(player))
              .setUnitType(Set.of(unitType));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit),
                  Set.of(unitSupportAttachment),
                  BattleState.Side.OFFENSE,
                  true));

      final Unit enemySupportUnit = unitType.createTemp(1, player).get(0);
      final UnitSupportAttachment enemyUnitSupportAttachment =
          givenUnitDefenseSupportAttachment(gameData, unitType, "test2")
              .setBonus(-1)
              .setPlayers(List.of(player))
              .setUnitType(Set.of(unitType));

      final AvailableSupports enemySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(enemySupportUnit),
                  Set.of(enemyUnitSupportAttachment),
                  BattleState.Side.DEFENSE,
                  false));

      final MainDefenseCombatValue.MainDefenseRoll roll =
          new MainDefenseCombatValue.MainDefenseRoll(friendlySupport, enemySupport);
      roll.getRoll(unit);
      assertThat(
          "Friendly gave 2 and enemy gave -1",
          roll.getSupportGiven(),
          is(
              Map.of(
                  supportUnit,
                  IntegerMap.of(Map.of(unit, 2)),
                  enemySupportUnit,
                  IntegerMap.of(Map.of(unit, -1)))));
    }
  }

  @Nested
  class MainDefenseStrengthTest {

    @Test
    void calculatesValue() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();

      final GamePlayer player = mock(GamePlayer.class);

      final UnitType unitType = new UnitType("test", gameData);
      final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      final Unit unit = unitType.createTemp(1, player).get(0);
      unit.getUnitAttachment().setDefense(3);

      final Unit supportUnit = unitType.createTemp(1, player).get(0);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, unitType, "test")
              .setBonus(3)
              .setPlayers(List.of(player))
              .setUnitType(Set.of(unitType));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit),
                  Set.of(unitSupportAttachment),
                  BattleState.Side.OFFENSE,
                  true));

      final Unit enemySupportUnit = unitType.createTemp(1, player).get(0);
      final UnitSupportAttachment enemyUnitSupportAttachment =
          givenUnitSupportAttachment(gameData, unitType, "test2")
              .setBonus(-2)
              .setPlayers(List.of(player))
              .setUnitType(Set.of(unitType));

      final AvailableSupports enemySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(enemySupportUnit),
                  Set.of(enemyUnitSupportAttachment),
                  BattleState.Side.OFFENSE,
                  true));

      final TerritoryEffect territoryEffect = new TerritoryEffect("territoryEffect", gameData);
      final TerritoryEffectAttachment territoryEffectAttachment =
          new TerritoryEffectAttachment("territoryEffectAttachment", territoryEffect, gameData);
      territoryEffect.addAttachment(TERRITORYEFFECT_ATTACHMENT_NAME, territoryEffectAttachment);
      territoryEffectAttachment.setCombatDefenseEffect(new IntegerMap<>(Map.of(unit.getType(), 1)));

      final MainDefenseCombatValue.MainDefenseStrength strength =
          new MainDefenseCombatValue.MainDefenseStrength(
              gameData.getSequence(), 6, List.of(territoryEffect), friendlySupport, enemySupport);
      assertThat(
          "Strength starts at 3, friendly adds 3, enemy removes 2, territory adds 1: total 5",
          strength.getStrength(unit).getValue(),
          is(5));
    }

    UnitSupportAttachment givenUnitSupportAttachment(
        final GameData gameData, final UnitType unitType, final String name)
        throws GameParseException {
      return new UnitSupportAttachment("rule" + name, unitType, gameData)
          .setBonus(1)
          .setBonusType("bonus" + name)
          .setDice("strength")
          .setNumber(1)
          .setSide("offence")
          .setFaction("allied");
    }

    @Test
    void calculatesValueWithFirstTurnLimited() throws GameParseException {
      final GamePlayer attacker = mock(GamePlayer.class);
      final RulesAttachment rulesAttachment = mock(RulesAttachment.class);
      when(rulesAttachment.getDominatingFirstRoundAttack()).thenReturn(true);
      when(attacker.getRulesAttachment()).thenReturn(rulesAttachment);

      final GameData gameData = givenGameData().withDiceSides(6).withRound(1, attacker).build();

      final GamePlayer player = mock(GamePlayer.class);

      final UnitType unitType = new UnitType("test", gameData);
      final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      final Unit unit = unitType.createTemp(1, player).get(0);
      unit.getUnitAttachment().setDefense(3);

      final Unit supportUnit = unitType.createTemp(1, player).get(0);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, unitType, "test")
              .setBonus(3)
              .setPlayers(List.of(player))
              .setUnitType(Set.of(unitType));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit),
                  Set.of(unitSupportAttachment),
                  BattleState.Side.OFFENSE,
                  true));

      final Unit enemySupportUnit = unitType.createTemp(1, player).get(0);
      final UnitSupportAttachment enemyUnitSupportAttachment =
          givenUnitSupportAttachment(gameData, unitType, "test2")
              .setBonus(-2)
              .setPlayers(List.of(player))
              .setUnitType(Set.of(unitType));

      final AvailableSupports enemySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(enemySupportUnit),
                  Set.of(enemyUnitSupportAttachment),
                  BattleState.Side.OFFENSE,
                  true));

      final TerritoryEffect territoryEffect = new TerritoryEffect("territoryEffect", gameData);
      final TerritoryEffectAttachment territoryEffectAttachment =
          new TerritoryEffectAttachment("territoryEffectAttachment", territoryEffect, gameData);
      territoryEffect.addAttachment(TERRITORYEFFECT_ATTACHMENT_NAME, territoryEffectAttachment);
      territoryEffectAttachment.setCombatDefenseEffect(new IntegerMap<>(Map.of(unit.getType(), 3)));

      final MainDefenseCombatValue.MainDefenseStrength strength =
          new MainDefenseCombatValue.MainDefenseStrength(
              gameData.getSequence(), 6, List.of(territoryEffect), friendlySupport, enemySupport);
      assertThat(
          "Strength is limited to 1, friendly is not used, "
              + "enemy removes 2, territory adds 3: total 1",
          strength.getStrength(unit).getValue(),
          is(2));
    }

    @Test
    void calculatesSupportUsed() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();

      final GamePlayer player = mock(GamePlayer.class);

      final UnitType unitType = new UnitType("test", gameData);
      final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      final Unit unit = unitType.createTemp(1, player).get(0);
      unit.getUnitAttachment().setDefense(3);

      final Unit supportUnit = unitType.createTemp(1, player).get(0);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, unitType, "test")
              .setBonus(2)
              .setPlayers(List.of(player))
              .setUnitType(Set.of(unitType));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(supportUnit),
                  Set.of(unitSupportAttachment),
                  BattleState.Side.OFFENSE,
                  true));

      final Unit enemySupportUnit = unitType.createTemp(1, player).get(0);
      final UnitSupportAttachment enemyUnitSupportAttachment =
          givenUnitSupportAttachment(gameData, unitType, "test2")
              .setBonus(-1)
              .setPlayers(List.of(player))
              .setUnitType(Set.of(unitType));

      final AvailableSupports enemySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(enemySupportUnit),
                  Set.of(enemyUnitSupportAttachment),
                  BattleState.Side.OFFENSE,
                  true));

      final MainDefenseCombatValue.MainDefenseStrength strength =
          new MainDefenseCombatValue.MainDefenseStrength(
              gameData.getSequence(), 6, List.of(), friendlySupport, enemySupport);
      strength.getStrength(unit);
      assertThat(
          "Friendly gave 2 and enemy gave -1",
          strength.getSupportGiven(),
          is(
              Map.of(
                  supportUnit,
                  IntegerMap.of(Map.of(unit, 2)),
                  enemySupportUnit,
                  IntegerMap.of(Map.of(unit, -1)))));
    }
  }
}
