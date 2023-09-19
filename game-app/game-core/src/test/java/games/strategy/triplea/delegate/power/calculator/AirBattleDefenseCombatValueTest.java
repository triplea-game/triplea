package games.strategy.triplea.delegate.power.calculator;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.IntegerMap;

class AirBattleDefenseCombatValueTest {

  @Nested
  class AirBattleDefenseStrengthTest {

    @Test
    void calculatesValue() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();

      final GamePlayer player = mock(GamePlayer.class);

      final UnitType unitType = new UnitType("test", gameData);
      final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      final Unit unit = unitType.createTemp(1, player).get(0);
      unit.getUnitAttachment().setAirDefense(3);

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

      final AirBattleDefenseCombatValue.AirBattleDefenseStrength strength =
          new AirBattleDefenseCombatValue.AirBattleDefenseStrength(
              6, friendlySupport, enemySupport);
      assertThat(
          "Strength starts at 3, friendly adds 3, enemy removes 2: total 4",
          strength.getStrength(unit).getValue(),
          is(4));
    }

    UnitSupportAttachment givenUnitSupportAttachment(
        final GameData gameData, final UnitType unitType, final String name)
        throws GameParseException {
      return new UnitSupportAttachment("rule" + name, unitType, gameData)
          .setBonus(1)
          .setBonusType("bonus" + name)
          .setDice("airStrength")
          .setNumber(1)
          .setSide("offence")
          .setFaction("allied");
    }

    @Test
    void limitsToDiceSides() {
      final GameData gameData = givenGameData().withDiceSides(6).build();

      final GamePlayer player = mock(GamePlayer.class);

      final UnitType unitType = new UnitType("test", gameData);
      final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      final Unit unit = unitType.createTemp(1, player).get(0);
      unit.getUnitAttachment().setAirDefense(8);

      final AirBattleDefenseCombatValue.AirBattleDefenseStrength strength =
              new AirBattleDefenseCombatValue.AirBattleDefenseStrength(6, AvailableSupports.EMPTY_RESULT, AvailableSupports.EMPTY_RESULT);
      assertThat(
              "Air defense is 8 but dice sides is 6 so it is limited to 6",
              strength.getStrength(unit).getValue(),
              is(6));
    }

    @Test
    void calculatesSupportUsed() throws GameParseException {
      final GameData gameData = givenGameData().withDiceSides(6).build();

      final GamePlayer player = mock(GamePlayer.class);

      final UnitType unitType = new UnitType("test", gameData);
      final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      final Unit unit = unitType.createTemp(1, player).get(0);
      unit.getUnitAttachment().setAirDefense(3);

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

      final AirBattleDefenseCombatValue.AirBattleDefenseStrength strength =
          new AirBattleDefenseCombatValue.AirBattleDefenseStrength(
              6, friendlySupport, enemySupport);
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
