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
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.IntegerMap;

class NormalOffenseStrengthTest {

  @Test
  void calculatesValue() throws GameParseException {
    final GamePlayer player = mock(GamePlayer.class);

    final GameData gameData = givenGameData().withDiceSides(6).build();

    final UnitType unitType = new UnitType("test", gameData);
    final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    final Unit unit = unitType.create(1, player, true).get(0);
    unit.getUnitAttachment().setAttack(3);

    final Unit supportUnit = mock(Unit.class);
    final UnitSupportAttachment unitSupportAttachment =
        new UnitSupportAttachment("rule", unitType, gameData)
            .setBonus(3)
            .setBonusType("bonus")
            .setDice("strength")
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
            .setBonus(-2)
            .setBonusType("bonus")
            .setDice("strength")
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

    final TerritoryEffect territoryEffect = new TerritoryEffect("territoryEffect", gameData);
    final TerritoryEffectAttachment territoryEffectAttachment =
        new TerritoryEffectAttachment("territoryEffectAttachment", territoryEffect, gameData);
    territoryEffect.addAttachment(TERRITORYEFFECT_ATTACHMENT_NAME, territoryEffectAttachment);
    territoryEffectAttachment.setCombatOffenseEffect(new IntegerMap<>(Map.of(unit.getType(), 1)));

    final NormalOffenseStrength strength =
        new NormalOffenseStrength(
            gameData, friendlySupport, enemySupport, List.of(territoryEffect), true);
    assertThat(
        "Strength starts at 3, friendly adds 3, enemy removes 2, territory adds 1: total 5",
        strength.getValue(unit),
        is(5));
  }

  @Test
  void addsMarineBonusIfAmphibious() {
    final GamePlayer player = mock(GamePlayer.class);

    final GameData gameData = givenGameData().withDiceSides(6).build();

    final UnitType unitType = new UnitType("test", gameData);
    final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    final Unit unit = unitType.create(1, player, true).get(0);
    unit.setWasAmphibious(true).getUnitAttachment().setAttack(3).setIsMarine(1);

    final NormalOffenseStrength strength =
        new NormalOffenseStrength(
            gameData,
            AvailableSupportCalculator.EMPTY_RESULT,
            AvailableSupportCalculator.EMPTY_RESULT,
            List.of(),
            true);
    assertThat("Strength starts at 3, marine adds 1: total 4", strength.getValue(unit), is(4));
  }

  @Test
  void ignoresMarineBonusIfNotAmphibious() {
    final GamePlayer player = mock(GamePlayer.class);

    final GameData gameData = givenGameData().withDiceSides(6).build();

    final UnitType unitType = new UnitType("test", gameData);
    final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    final Unit unit = unitType.create(1, player, true).get(0);
    unit.getUnitAttachment().setAttack(3).setIsMarine(1);

    final NormalOffenseStrength strength =
        new NormalOffenseStrength(
            gameData,
            AvailableSupportCalculator.EMPTY_RESULT,
            AvailableSupportCalculator.EMPTY_RESULT,
            List.of(),
            true);
    assertThat(
        "Strength starts at 3 and marine is not added: total 3", strength.getValue(unit), is(3));
  }

  @Test
  void bombardUsedIfLandBattleAndSeaUnit() {
    final GamePlayer player = mock(GamePlayer.class);

    final GameData gameData = givenGameData().withDiceSides(6).build();

    final UnitType unitType = new UnitType("test", gameData);
    final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    final Unit unit = unitType.create(1, player, true).get(0);
    unit.getUnitAttachment().setAttack(3).setBombard(1).setIsSea(true);

    final NormalOffenseStrength strength =
        new NormalOffenseStrength(
            gameData,
            AvailableSupportCalculator.EMPTY_RESULT,
            AvailableSupportCalculator.EMPTY_RESULT,
            List.of(),
            true);
    assertThat("Bombard is 1", strength.getValue(unit), is(1));
  }

  @Test
  void bombardNotUsedIfNotLandBattle() {
    final GamePlayer player = mock(GamePlayer.class);

    final GameData gameData = givenGameData().withDiceSides(6).build();

    final UnitType unitType = new UnitType("test", gameData);
    final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    final Unit unit = unitType.create(1, player, true).get(0);
    unit.getUnitAttachment().setAttack(3).setBombard(1).setIsSea(true);

    final NormalOffenseStrength strength =
        new NormalOffenseStrength(
            gameData,
            AvailableSupportCalculator.EMPTY_RESULT,
            AvailableSupportCalculator.EMPTY_RESULT,
            List.of(),
            false);
    assertThat("Regular attack is 3", strength.getValue(unit), is(3));
  }

  @Test
  void bombardNotUsedIfNotSeaUnitOnLandBattle() {
    final GamePlayer player = mock(GamePlayer.class);

    final GameData gameData = givenGameData().withDiceSides(6).build();

    final UnitType unitType = new UnitType("test", gameData);
    final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    final Unit unit = unitType.create(1, player, true).get(0);
    unit.getUnitAttachment().setAttack(3).setBombard(1).setIsSea(false);

    final NormalOffenseStrength strength =
        new NormalOffenseStrength(
            gameData,
            AvailableSupportCalculator.EMPTY_RESULT,
            AvailableSupportCalculator.EMPTY_RESULT,
            List.of(),
            true);
    assertThat("Regular attack is 3", strength.getValue(unit), is(3));
  }

  @Test
  void calculatesSupportUsed() throws GameParseException {
    final GamePlayer player = mock(GamePlayer.class);

    final GameData gameData = givenGameData().withDiceSides(6).build();

    final UnitType unitType = new UnitType("test", gameData);
    final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    final Unit unit = unitType.create(1, player, true).get(0);
    unit.getUnitAttachment().setAttack(3);

    final Unit supportUnit = mock(Unit.class);
    final UnitSupportAttachment unitSupportAttachment =
        new UnitSupportAttachment("rule", unitType, gameData)
            .setBonus(2)
            .setBonusType("bonus")
            .setDice("strength")
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
            .setDice("strength")
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

    final NormalOffenseStrength strength =
        new NormalOffenseStrength(gameData, friendlySupport, enemySupport, List.of(), true);
    strength.getValue(unit);
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
