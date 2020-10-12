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

class AaDefenseRollTest {

  @Test
  void calculatesValue() throws GameParseException {
    final GamePlayer player = mock(GamePlayer.class);

    final GameData gameData = givenGameData().build();

    final UnitType unitType = new UnitType("test", gameData);
    final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    final Unit unit = unitType.create(1, player, true).get(0);
    unit.getUnitAttachment().setMaxAaAttacks(3);

    final Unit supportUnit = mock(Unit.class);
    final UnitSupportAttachment unitSupportAttachment =
        new UnitSupportAttachment("rule", unitType, gameData)
            .setBonus(2)
            .setBonusType("bonus")
            .setDice("AAroll")
            .setUnitType(Set.of(unitType));

    final AvailableSupportCalculator friendlySupport =
        AvailableSupportCalculator.builder()
            .supportRules(
                Map.of(
                    new UnitSupportAttachment.BonusType("bonus", 1),
                    List.of(unitSupportAttachment)))
            .supportUnits(Map.of(unitSupportAttachment, new IntegerMap<>(Map.of(supportUnit, 1))))
            .build();

    final Unit enemySupportUnit = mock(Unit.class);
    final UnitSupportAttachment enemyUnitSupportAttachment =
        new UnitSupportAttachment("rule2", unitType, gameData)
            .setBonus(-1)
            .setBonusType("bonus")
            .setDice("AAroll")
            .setUnitType(Set.of(unitType));

    final AvailableSupportCalculator enemySupport =
        AvailableSupportCalculator.builder()
            .supportRules(
                Map.of(
                    new UnitSupportAttachment.BonusType("bonus", 1),
                    List.of(enemyUnitSupportAttachment)))
            .supportUnits(
                Map.of(enemyUnitSupportAttachment, new IntegerMap<>(Map.of(enemySupportUnit, 1))))
            .build();

    final AaDefenseRoll roll = new AaDefenseRoll(friendlySupport, enemySupport);
    assertThat(
        "Roll starts at 3, friendly adds 2, enemy removes 1: total 4", roll.getValue(unit), is(4));
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

    final Unit supportUnit = mock(Unit.class);
    final UnitSupportAttachment unitSupportAttachment =
        new UnitSupportAttachment("rule", unitType, gameData)
            .setBonus(2)
            .setBonusType("bonus")
            .setDice("AAroll")
            .setUnitType(Set.of(unitType));

    final AvailableSupportCalculator friendlySupport =
        AvailableSupportCalculator.builder()
            .supportRules(
                Map.of(
                    new UnitSupportAttachment.BonusType("bonus", 1),
                    List.of(unitSupportAttachment)))
            .supportUnits(Map.of(unitSupportAttachment, new IntegerMap<>(Map.of(supportUnit, 1))))
            .build();

    final Unit enemySupportUnit = mock(Unit.class);
    final UnitSupportAttachment enemyUnitSupportAttachment =
        new UnitSupportAttachment("rule2", unitType, gameData)
            .setBonus(-1)
            .setBonusType("bonus")
            .setDice("AAroll")
            .setUnitType(Set.of(unitType));

    final AvailableSupportCalculator enemySupport =
        AvailableSupportCalculator.builder()
            .supportRules(
                Map.of(
                    new UnitSupportAttachment.BonusType("bonus", 1),
                    List.of(enemyUnitSupportAttachment)))
            .supportUnits(
                Map.of(enemyUnitSupportAttachment, new IntegerMap<>(Map.of(enemySupportUnit, 1))))
            .build();

    final AaDefenseRoll roll = new AaDefenseRoll(friendlySupport, enemySupport);
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
