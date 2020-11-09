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

class BombardmentCombatValueTest {

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
      unit.getUnitAttachment().setBombard(3);

      final Unit supportUnit = unitType.create(1, player, true).get(0);
      final UnitSupportAttachment unitSupportAttachment =
          givenUnitOffenseSupportAttachment(gameData, unitType, "test")
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

      final Unit enemySupportUnit = unitType.create(1, player, true).get(0);
      final UnitSupportAttachment enemyUnitSupportAttachment =
          givenUnitDefenseSupportAttachment(gameData, unitType, "test2")
              .setBonus(-2)
              .setPlayers(List.of(player))
              .setUnitType(Set.of(unitType));

      final AvailableSupports enemySupport =
          AvailableSupports.getSupport(
              new SupportCalculator(
                  List.of(enemySupportUnit),
                  Set.of(enemyUnitSupportAttachment),
                  BattleState.Side.DEFENSE,
                  false));

      final TerritoryEffect territoryEffect = new TerritoryEffect("territoryEffect", gameData);
      final TerritoryEffectAttachment territoryEffectAttachment =
          new TerritoryEffectAttachment("territoryEffectAttachment", territoryEffect, gameData);
      territoryEffect.addAttachment(TERRITORYEFFECT_ATTACHMENT_NAME, territoryEffectAttachment);
      territoryEffectAttachment.setCombatOffenseEffect(new IntegerMap<>(Map.of(unit.getType(), 1)));

      final BombardmentCombatValue.BombardmentStrength strength =
          new BombardmentCombatValue.BombardmentStrength(
              6, List.of(territoryEffect), friendlySupport, enemySupport);
      assertThat(
          "Strength starts at 3, friendly adds 3, enemy removes 2, territory adds 1: total 5",
          strength.getStrength(unit).getValue(),
          is(5));
    }

    UnitSupportAttachment givenUnitOffenseSupportAttachment(
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

    UnitSupportAttachment givenUnitDefenseSupportAttachment(
        final GameData gameData, final UnitType unitType, final String name)
        throws GameParseException {
      return new UnitSupportAttachment("rule" + name, unitType, gameData)
          .setBonus(1)
          .setBonusType("bonus" + name)
          .setDice("strength")
          .setNumber(1)
          .setSide("defence")
          .setFaction("enemy");
    }
  }
}
