package games.strategy.triplea.delegate.power.calculator;

import static games.strategy.triplea.Constants.RULES_ATTACHMENT_NAME;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.IntegerMap;

class NormalDefenseStrengthTest {

  @Test
  void calculatesValue() throws GameParseException {
    final GameData gameData = givenGameData().withDiceSides(6).build();

    final GamePlayer player = mock(GamePlayer.class);

    final UnitType unitType = new UnitType("test", gameData);
    final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    final Unit unit = unitType.create(1, player, true).get(0);
    unit.getUnitAttachment().setDefense(3);

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
    territoryEffectAttachment.setCombatDefenseEffect(new IntegerMap<>(Map.of(unit.getType(), 1)));

    final NormalDefenseStrength strength =
        new NormalDefenseStrength(
            gameData, friendlySupport, enemySupport, List.of(territoryEffect));
    assertThat(
        "Strength starts at 3, friendly adds 3, enemy removes 2, territory adds 1: total 5",
        strength.getValue(unit),
        is(5));
  }

  @Test
  void calculatesValueWithFirstTurnLimited() throws GameParseException {
    final GamePlayer attacker = mock(GamePlayer.class);
    final RulesAttachment rulesAttachment = mock(RulesAttachment.class);
    when(rulesAttachment.getDominatingFirstRoundAttack()).thenReturn(true);
    when(attacker.getAttachment(RULES_ATTACHMENT_NAME)).thenReturn(rulesAttachment);

    final GameData gameData = givenGameData().withDiceSides(6).withRound(1, attacker).build();

    final GamePlayer player = mock(GamePlayer.class);

    final UnitType unitType = new UnitType("test", gameData);
    final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    final Unit unit = unitType.create(1, player, true).get(0);
    unit.getUnitAttachment().setDefense(3);

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
    territoryEffectAttachment.setCombatDefenseEffect(new IntegerMap<>(Map.of(unit.getType(), 3)));

    final NormalDefenseStrength strength =
        new NormalDefenseStrength(
            gameData, friendlySupport, enemySupport, List.of(territoryEffect));
    assertThat(
        "Strength is limited to 1, friendly is not used, "
            + "enemy removes 2, territory adds 3: total 1",
        strength.getValue(unit),
        is(2));
  }
}
