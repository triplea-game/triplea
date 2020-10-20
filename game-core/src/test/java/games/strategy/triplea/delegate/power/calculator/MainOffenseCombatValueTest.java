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
      final Unit unit = unitType.create(1, player, true).get(0);
      unit.getUnitAttachment().setAttackRolls(3);

      final Unit supportUnit = unitType.create(1, player, true).get(0);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, unitType, "test")
              .setBonus(2)
              .setPlayers(List.of(player))
              .setUnitType(Set.of(unitType));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              List.of(supportUnit), Set.of(unitSupportAttachment), false, true);

      final Unit enemySupportUnit = unitType.create(1, player, true).get(0);
      final UnitSupportAttachment enemyUnitSupportAttachment =
          givenUnitSupportAttachment(gameData, unitType, "test2")
              .setBonus(-1)
              .setPlayers(List.of(player))
              .setUnitType(Set.of(unitType));

      final AvailableSupports enemySupport =
          AvailableSupports.getSupport(
              List.of(enemySupportUnit), Set.of(enemyUnitSupportAttachment), false, true);

      final MainOffenseCombatValue.MainOffenseRoll roll =
          new MainOffenseCombatValue.MainOffenseRoll(friendlySupport, enemySupport);
      assertThat(
          "Roll starts at 3, friendly adds 2, enemy removes 1: total 4",
          roll.getValue(unit),
          is(4));
    }

    UnitSupportAttachment givenUnitSupportAttachment(
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

    @Test
    void calculatesSupportUsed() throws GameParseException {
      final GamePlayer player = mock(GamePlayer.class);

      final GameData gameData = givenGameData().build();

      final UnitType unitType = new UnitType("test", gameData);
      final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      final Unit unit = unitType.create(1, player, true).get(0);
      unit.getUnitAttachment().setAttackRolls(3);

      final Unit supportUnit = unitType.create(1, player, true).get(0);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, unitType, "test")
              .setBonus(2)
              .setPlayers(List.of(player))
              .setUnitType(Set.of(unitType));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              List.of(supportUnit), Set.of(unitSupportAttachment), false, true);

      final Unit enemySupportUnit = unitType.create(1, player, true).get(0);
      final UnitSupportAttachment enemyUnitSupportAttachment =
          givenUnitSupportAttachment(gameData, unitType, "test2")
              .setBonus(-1)
              .setPlayers(List.of(player))
              .setUnitType(Set.of(unitType));

      final AvailableSupports enemySupport =
          AvailableSupports.getSupport(
              List.of(enemySupportUnit), Set.of(enemyUnitSupportAttachment), false, true);

      final MainOffenseCombatValue.MainOffenseRoll roll =
          new MainOffenseCombatValue.MainOffenseRoll(friendlySupport, enemySupport);
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

  @Nested
  class MainOffenseStrengthTest {

    @Test
    void calculatesValue() throws GameParseException {
      final GamePlayer player = mock(GamePlayer.class);

      final GameData gameData = givenGameData().withDiceSides(6).build();

      final UnitType unitType = new UnitType("test", gameData);
      final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      final Unit unit = unitType.create(1, player, true).get(0);
      unit.getUnitAttachment().setAttack(3);

      final Unit supportUnit = unitType.create(1, player, true).get(0);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, unitType, "test")
              .setBonus(3)
              .setPlayers(List.of(player))
              .setUnitType(Set.of(unitType));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              List.of(supportUnit), Set.of(unitSupportAttachment), false, true);

      final Unit enemySupportUnit = unitType.create(1, player, true).get(0);
      final UnitSupportAttachment enemyUnitSupportAttachment =
          givenUnitSupportAttachment(gameData, unitType, "test2")
              .setBonus(-2)
              .setPlayers(List.of(player))
              .setUnitType(Set.of(unitType));

      final AvailableSupports enemySupport =
          AvailableSupports.getSupport(
              List.of(enemySupportUnit), Set.of(enemyUnitSupportAttachment), false, true);

      final TerritoryEffect territoryEffect = new TerritoryEffect("territoryEffect", gameData);
      final TerritoryEffectAttachment territoryEffectAttachment =
          new TerritoryEffectAttachment("territoryEffectAttachment", territoryEffect, gameData);
      territoryEffect.addAttachment(TERRITORYEFFECT_ATTACHMENT_NAME, territoryEffectAttachment);
      territoryEffectAttachment.setCombatOffenseEffect(new IntegerMap<>(Map.of(unit.getType(), 1)));

      final MainOffenseCombatValue.MainOffenseStrength strength =
          new MainOffenseCombatValue.MainOffenseStrength(
              gameData, friendlySupport, enemySupport, List.of(territoryEffect), true);
      assertThat(
          "Strength starts at 3, friendly adds 3, enemy removes 2, territory adds 1: total 5",
          strength.getValue(unit),
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
      final Unit unit = unitType.create(1, player, true).get(0);
      unit.setWasAmphibious(true).getUnitAttachment().setAttack(3).setIsMarine(1);

      final MainOffenseCombatValue.MainOffenseStrength strength =
          new MainOffenseCombatValue.MainOffenseStrength(
              gameData,
              AvailableSupports.EMPTY_RESULT,
              AvailableSupports.EMPTY_RESULT,
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

      final MainOffenseCombatValue.MainOffenseStrength strength =
          new MainOffenseCombatValue.MainOffenseStrength(
              gameData,
              AvailableSupports.EMPTY_RESULT,
              AvailableSupports.EMPTY_RESULT,
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

      final MainOffenseCombatValue.MainOffenseStrength strength =
          new MainOffenseCombatValue.MainOffenseStrength(
              gameData,
              AvailableSupports.EMPTY_RESULT,
              AvailableSupports.EMPTY_RESULT,
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

      final MainOffenseCombatValue.MainOffenseStrength strength =
          new MainOffenseCombatValue.MainOffenseStrength(
              gameData,
              AvailableSupports.EMPTY_RESULT,
              AvailableSupports.EMPTY_RESULT,
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

      final MainOffenseCombatValue.MainOffenseStrength strength =
          new MainOffenseCombatValue.MainOffenseStrength(
              gameData,
              AvailableSupports.EMPTY_RESULT,
              AvailableSupports.EMPTY_RESULT,
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

      final Unit supportUnit = unitType.create(1, player, true).get(0);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitSupportAttachment(gameData, unitType, "test")
              .setBonus(2)
              .setPlayers(List.of(player))
              .setUnitType(Set.of(unitType));

      final AvailableSupports friendlySupport =
          AvailableSupports.getSupport(
              List.of(supportUnit), Set.of(unitSupportAttachment), false, true);

      final Unit enemySupportUnit = unitType.create(1, player, true).get(0);
      final UnitSupportAttachment enemyUnitSupportAttachment =
          givenUnitSupportAttachment(gameData, unitType, "test2")
              .setBonus(-1)
              .setPlayers(List.of(player))
              .setUnitType(Set.of(unitType));

      final AvailableSupports enemySupport =
          AvailableSupports.getSupport(
              List.of(enemySupportUnit), Set.of(enemyUnitSupportAttachment), false, true);

      final MainOffenseCombatValue.MainOffenseStrength strength =
          new MainOffenseCombatValue.MainOffenseStrength(
              gameData, friendlySupport, enemySupport, List.of(), true);
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
}
