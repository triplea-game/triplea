package games.strategy.triplea.delegate.battle.casualty;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameSequence;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.UnitTypeList;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.java.collections.IntegerMap;

@ExtendWith(MockitoExtension.class)
class CasualtyOrderOfLossesTest {

  @Mock GameData gameData;
  @Mock UnitTypeList unitTypeList;
  @Mock GamePlayer player;
  @Mock UnitAttachment unitAttachment;

  @BeforeEach
  void clearCache() {
    CasualtyOrderOfLosses.clearOolCache();
  }

  @Test
  void oolCacheKeyIsUniqueWhenUnitTypeHashCodesHaveSameSum() {
    final UnitType typePikemen = new UnitType("Pikemen", gameData);
    typePikemen.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    final UnitType typeFootmen = new UnitType("Footmen", gameData);
    typeFootmen.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    final UnitType typeVeteranPikemen = new UnitType("Veteran-Pikemen", gameData);
    typeVeteranPikemen.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    final UnitType typeVeteranFootmen = new UnitType("Veteran-Footmen", gameData);
    typeVeteranFootmen.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);

    final String key1 =
        CasualtyOrderOfLosses.computeOolCacheKey(
            withFakeParameters(),
            List.of(
                CasualtyOrderOfLosses.AmphibType.of(typePikemen.create(1, player, true).get(0)),
                CasualtyOrderOfLosses.AmphibType.of(
                    typeVeteranFootmen.create(1, player, true).get(0))));

    final String key2 =
        CasualtyOrderOfLosses.computeOolCacheKey(
            withFakeParameters(),
            List.of(
                CasualtyOrderOfLosses.AmphibType.of(typeFootmen.create(1, player, true).get(0)),
                CasualtyOrderOfLosses.AmphibType.of(
                    typeVeteranPikemen.create(1, player, true).get(0))));

    assertThat(key1, is(not(key2)));
  }

  private CasualtyOrderOfLosses.Parameters withFakeParameters() {
    final GamePlayer player = mock(GamePlayer.class);
    when(player.getName()).thenReturn("player");
    final Territory territory = mock(Territory.class);
    when(territory.getName()).thenReturn("territory");
    return CasualtyOrderOfLosses.Parameters.builder()
        .targetsToPickFrom(List.of())
        .player(player)
        .combatValue(
            CombatValueBuilder.mainCombatValue()
                .enemyUnits(List.of())
                .friendlyUnits(List.of())
                .side(BattleState.Side.OFFENSE)
                .gameSequence(mock(GameSequence.class))
                .supportAttachments(List.of())
                .lhtrHeavyBombers(false)
                .gameDiceSides(gameData.getDiceSides())
                .territoryEffects(List.of())
                .build())
        .battlesite(territory)
        .costs(IntegerMap.of(Map.of()))
        .data(gameData)
        .build();
  }

  /*
  @Test
  void twoUnitsWithHigherPowerSupportsAnotherUnit() throws GameParseException {
    final GameData gameData = givenGameData().withDiceSides(6).build();

    final GamePlayer owner = mock(GamePlayer.class);

    final UnitType unitType1 = new UnitType("unit1", gameData);
    final UnitAttachment unitAttachment1 = new UnitAttachment("attachment1", unitType1, gameData);
    unitAttachment1.setAttack(2).setAttackRolls(1);
    unitType1.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment1);
    final Unit unit1 = unitType1.create(1, owner, true).get(0);
    final Unit unit1NonSupporting = unitType1.create(1, owner, true).get(0);

    final UnitType unitType2 = new UnitType("unit2", gameData);
    final UnitAttachment unitAttachment2 = new UnitAttachment("attachment2", unitType2, gameData);
    unitAttachment2.setAttack(1).setAttackRolls(1);
    unitType2.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment2);
    final Unit unit2 = unitType2.create(1, owner, true).get(0);

    final UnitSupportAttachment rule1 =
        new UnitSupportAttachment("rule", unitType1, gameData);
    rule1.setSide("offence")
        .setFaction("allied")
        .setPlayers(List.of(owner))
        .setBonusType("bonus")
        .setUnitType(Set.of(unitType2))
        .setBonus(2)
        .setNumber(1);

    final List<Unit> units = List.of(unit1NonSupporting, unit1, unit2);

    final List<Unit> sortedUnits = CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(
        CasualtyOrderOfLosses.Parameters.builder()
            .targetsToPickFrom(units)
            .player(owner)
            .combatValue(
                CombatValueBuilder.mainCombatValue()
                    .enemyUnits(List.of())
                    .friendlyUnits(units)
                    .side(BattleState.Side.OFFENSE)
                    .gameSequence(mock(GameSequence.class))
                    .supportAttachments(List.of())
                    .lhtrHeavyBombers(false)
                    .gameDiceSides(6)
                    .territoryEffects(List.of())
                    .build())
            .battlesite(mock(Territory.class))
            .costs(IntegerMap.of(Map.of()))
            .data(gameData)
            .build()
    );

    assertThat(
        "unitType2 has equal power to unitType1 if supported so take the unit1 that isn't giving "
            + "support first, then unit2, then unit1",
        sortedUnits.stream().map(Unit::getType).collect(Collectors.toList()),
        is(List.of(unitType1, unitType2, unitType1)));
  }

  @Test
  void unitTypesSupportEachOther() throws GameParseException {
    final GameData gameData = givenGameData().withDiceSides(6).build();

    final GamePlayer owner = mock(GamePlayer.class);

    final UnitType unitType1 = new UnitType("unit1", gameData);
    final UnitAttachment unitAttachment1 = new UnitAttachment("attachment1", unitType1, gameData);
    unitAttachment1.setAttack(1).setAttackRolls(1);
    unitType1.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment1);
    final Unit unit1a = unitType1.create(1, owner, true).get(0);
    final Unit unit1b = unitType1.create(1, owner, true).get(0);

    final UnitType unitType2 = new UnitType("unit2", gameData);
    final UnitAttachment unitAttachment2 = new UnitAttachment("attachment2", unitType2, gameData);
    unitAttachment2.setAttack(1).setAttackRolls(1);
    unitType2.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment2);
    final Unit unit2a = unitType2.create(1, owner, true).get(0);
    final Unit unit2b = unitType2.create(1, owner, true).get(0);

    final UnitType unitType3 = new UnitType("unit3", gameData);
    final UnitAttachment unitAttachment3 = new UnitAttachment("attachment3", unitType3, gameData);
    unitAttachment3.setAttack(2).setAttackRolls(1);
    unitType3.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment3);
    final Unit unit3a = unitType3.create(1, owner, true).get(0);
    final Unit unit3b = unitType3.create(1, owner, true).get(0);

    final UnitType unitType4 = new UnitType("unit4", gameData);
    final UnitAttachment unitAttachment4 = new UnitAttachment("attachment4", unitType4, gameData);
    unitAttachment4.setAttack(1).setAttackRolls(1);
    unitType4.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment4);
    final Unit unit4a = unitType4.create(1, owner, true).get(0);
    final Unit unit4b = unitType4.create(1, owner, true).get(0);

    final UnitSupportAttachment rule1 =
        new UnitSupportAttachment("rule1", unitType1, gameData);
    rule1.setSide("offence")
        .setFaction("allied")
        .setPlayers(List.of(owner))
        .setBonusType("bonus1")
        .setUnitType(Set.of(unitType2))
        .setBonus(1)
        .setNumber(1);

    final UnitSupportAttachment rule2 =
        new UnitSupportAttachment("rule2", unitType2, gameData);
    rule2.setSide("offence")
        .setFaction("allied")
        .setPlayers(List.of(owner))
        .setBonusType("bonus2")
        .setUnitType(Set.of(unitType1))
        .setBonus(1)
        .setNumber(1);

    final UnitSupportAttachment rule3 =
        new UnitSupportAttachment("rule3", unitType3, gameData);
    rule3.setSide("offence")
        .setFaction("allied")
        .setPlayers(List.of(owner))
        .setBonusType("bonus3")
        .setUnitType(Set.of(unitType1))
        .setBonus(1)
        .setNumber(1);

    final UnitSupportAttachment rule4 =
        new UnitSupportAttachment("rule4", unitType4, gameData);
    rule4.setSide("offence")
        .setFaction("allied")
        .setPlayers(List.of(owner))
        .setBonusType("bonus4")
        .setUnitType(Set.of(unitType3))
        .setBonus(1)
        .setNumber(1);

    final List<Unit> units = List.of(unit1a, unit1b, unit2a, unit2b, unit3a, unit3b, unit4a, unit4b);

    final List<Unit> sortedUnits = CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(
        CasualtyOrderOfLosses.Parameters.builder()
            .targetsToPickFrom(units)
            .player(owner)
            .combatValue(
                CombatValueBuilder.mainCombatValue()
                    .enemyUnits(List.of())
                    .friendlyUnits(units)
                    .side(BattleState.Side.OFFENSE)
                    .gameSequence(mock(GameSequence.class))
                    .supportAttachments(List.of())
                    .lhtrHeavyBombers(false)
                    .gameDiceSides(6)
                    .territoryEffects(List.of())
                    .build())
            .battlesite(mock(Territory.class))
            .costs(IntegerMap.of(Map.of()))
            .data(gameData)
            .build()
    );

    assertThat(
        sortedUnits.stream().map(Unit::getType).collect(Collectors.toList()),
        is(List.of(unitType4, unitType4, unitType2, unitType1, unitType3, unitType2, unitType1, unitType3)));
  }

   */
}
