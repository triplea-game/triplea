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
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.IntegerMap;

class AaRollTest {

  @Test
  void calculatesValue() throws GameParseException {
    final GamePlayer player = mock(GamePlayer.class);

    final GameData gameData = givenGameData().build();

    final UnitType unitType = new UnitType("test", gameData);
    final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    final Unit unit = unitType.create(1, player, true).get(0);
    unit.getUnitAttachment().setMaxAaAttacks(3);

    final Unit supportUnit = unitType.create(1, player, true).get(0);
    final UnitSupportAttachment unitSupportAttachment =
        givenUnitSupportAttachment(gameData, unitType, "test")
            .setBonus(2)
            .setPlayers(List.of(player))
            .setUnitType(Set.of(unitType));

    final AvailableSupports friendlySupport =
        AvailableSupports.getSupport(
            new SupportCalculator(
                List.of(supportUnit), Set.of(unitSupportAttachment), false, true));

    final Unit enemySupportUnit = unitType.create(1, player, true).get(0);
    final UnitSupportAttachment enemyUnitSupportAttachment =
        givenUnitSupportAttachment(gameData, unitType, "test2")
            .setBonus(-1)
            .setPlayers(List.of(player))
            .setUnitType(Set.of(unitType));

    final AvailableSupports enemySupport =
        AvailableSupports.getSupport(
            new SupportCalculator(
                List.of(enemySupportUnit), Set.of(enemyUnitSupportAttachment), false, true));

    final AaRoll roll = new AaRoll(friendlySupport, enemySupport);
    assertThat(
        "Roll starts at 3, friendly adds 2, enemy removes 1: total 4", roll.getValue(unit), is(4));
  }

  UnitSupportAttachment givenUnitSupportAttachment(
      final GameData gameData, final UnitType unitType, final String name)
      throws GameParseException {
    return new UnitSupportAttachment("rule" + name, unitType, gameData)
        .setBonus(1)
        .setBonusType("bonus" + name)
        .setDice("AAroll")
        .setNumber(1)
        .setSide("offence")
        .setFaction("allied");
  }

  @Test
  void calculatesSupportUsed() throws GameParseException {
    final GamePlayer player = mock(GamePlayer.class);

    final GameData gameData = givenGameData().build();

    final UnitType unitType = new UnitType("test", gameData);
    final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    final Unit unit = unitType.create(1, player, true).get(0);
    unit.getUnitAttachment().setMaxAaAttacks(3);

    final Unit supportUnit = unitType.create(1, player, true).get(0);
    final UnitSupportAttachment unitSupportAttachment =
        givenUnitSupportAttachment(gameData, unitType, "test")
            .setBonus(2)
            .setPlayers(List.of(player))
            .setUnitType(Set.of(unitType));

    final AvailableSupports friendlySupport =
        AvailableSupports.getSupport(
            new SupportCalculator(
                List.of(supportUnit), Set.of(unitSupportAttachment), false, true));

    final Unit enemySupportUnit = unitType.create(1, player, true).get(0);
    final UnitSupportAttachment enemyUnitSupportAttachment =
        givenUnitSupportAttachment(gameData, unitType, "test2")
            .setBonus(-1)
            .setPlayers(List.of(player))
            .setUnitType(Set.of(unitType));

    final AvailableSupports enemySupport =
        AvailableSupports.getSupport(
            new SupportCalculator(
                List.of(enemySupportUnit), Set.of(enemyUnitSupportAttachment), false, true));

    final AaRoll roll = new AaRoll(friendlySupport, enemySupport);
    roll.getValue(unit);
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
