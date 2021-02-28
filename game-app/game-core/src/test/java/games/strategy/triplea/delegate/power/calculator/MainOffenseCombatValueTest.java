package games.strategy.triplea.delegate.power.calculator;

import static games.strategy.triplea.Constants.TERRITORYEFFECT_ATTACHMENT_NAME;
import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.gameparser.GameParseException;
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

class MainOffenseCombatValueTest {

  @Nested
  class MainOffenseRollTest {

    @Test
    void calculatesValue() throws GameParseException {
      final GamePlayer player = mock(GamePlayer.class);

      final GameData gameData = givenGameData().build();

      final UnitType unitType = new UnitType("test", gameData);
      final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      final Unit unit = unitType.createTemp(1, player).get(0);
      unit.getUnitAttachment().setAttackRolls(3);

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

      final MainOffenseCombatValue.MainOffenseRoll roll =
          new MainOffenseCombatValue.MainOffenseRoll(friendlySupport, enemySupport);
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
      unit.getUnitAttachment().setAttackRolls(3);

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

      final MainOffenseCombatValue.MainOffenseRoll roll =
          new MainOffenseCombatValue.MainOffenseRoll(friendlySupport, enemySupport);
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
  class MainOffenseStrengthTest {

    @Test
    void calculatesValue() throws GameParseException {
      final GamePlayer player = mock(GamePlayer.class);

      final GameData gameData = givenGameData().withDiceSides(6).build();

      final UnitType unitType = new UnitType("test", gameData);
      final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      final Unit unit = unitType.createTemp(1, player).get(0);
      unit.getUnitAttachment().setAttack(3);

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
      territoryEffectAttachment.setCombatOffenseEffect(new IntegerMap<>(Map.of(unit.getType(), 1)));

      final MainOffenseCombatValue.MainOffenseStrength strength =
          new MainOffenseCombatValue.MainOffenseStrength(
              6, List.of(territoryEffect), friendlySupport, enemySupport);
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
    void addsMarineBonusIfAmphibious() {
      final GamePlayer player = mock(GamePlayer.class);

      final GameData gameData = givenGameData().withDiceSides(6).build();

      final UnitType unitType = new UnitType("test", gameData);
      final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      final Unit unit = unitType.createTemp(1, player).get(0);
      unit.setWasAmphibious(true).getUnitAttachment().setAttack(3).setIsMarine(1);

      final MainOffenseCombatValue.MainOffenseStrength strength =
          new MainOffenseCombatValue.MainOffenseStrength(
              6, List.of(), AvailableSupports.EMPTY_RESULT, AvailableSupports.EMPTY_RESULT);
      assertThat(
          "Strength starts at 3, marine adds 1: total 4",
          strength.getStrength(unit).getValue(),
          is(4));
    }

    @Test
    void ignoresMarineBonusIfNotAmphibious() {
      final GamePlayer player = mock(GamePlayer.class);

      final GameData gameData = givenGameData().withDiceSides(6).build();

      final UnitType unitType = new UnitType("test", gameData);
      final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      final Unit unit = unitType.createTemp(1, player).get(0);
      unit.getUnitAttachment().setAttack(3).setIsMarine(1);

      final MainOffenseCombatValue.MainOffenseStrength strength =
          new MainOffenseCombatValue.MainOffenseStrength(
              6, List.of(), AvailableSupports.EMPTY_RESULT, AvailableSupports.EMPTY_RESULT);
      assertThat(
          "Strength starts at 3 and marine is not added: total 3",
          strength.getStrength(unit).getValue(),
          is(3));
    }

    @Test
    void calculatesSupportUsed() throws GameParseException {
      final GamePlayer player = mock(GamePlayer.class);

      final GameData gameData = givenGameData().withDiceSides(6).build();

      final UnitType unitType = new UnitType("test", gameData);
      final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      final Unit unit = unitType.createTemp(1, player).get(0);
      unit.getUnitAttachment().setAttack(3);

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

      final MainOffenseCombatValue.MainOffenseStrength strength =
          new MainOffenseCombatValue.MainOffenseStrength(
              6, List.of(), friendlySupport, enemySupport);
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
