package games.strategy.triplea.delegate.battle.casualty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.ImprovedArtillerySupportAdvance;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import games.strategy.triplea.xml.TestDataBigWorld1942V3;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("SameParameterValue")
class CasualtyOrderOfLossesTestOnBigWorldV3 {

  private final TestDataBigWorld1942V3 testData = new TestDataBigWorld1942V3();

  @BeforeEach
  void clearCache() {
    CasualtyOrderOfLosses.clearOolCache();
  }

  @Test
  void improvedArtillery() {
    testData.addTech(new ImprovedArtillerySupportAdvance(testData.gameData));

    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(testData.tank(1));
    attackingUnits.addAll(testData.artillery(1));
    attackingUnits.addAll(testData.marine(1));
    attackingUnits.addAll(testData.marine(1));

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(amphibAssault(attackingUnits));

    assertThat(result, hasSize(4));
    assertThat(result.get(0).getType(), is(testData.tank));
    assertThat(result.get(1).getType(), is(testData.artillery));
    assertThat(result.get(2).getType(), is(testData.marine));
    assertThat(result.get(3).getType(), is(testData.marine));
  }

  private CasualtyOrderOfLosses.Parameters amphibAssault(final Collection<Unit> amphibUnits) {
    amphibUnits.forEach(
        unit ->
            unit.getProperty(Unit.PropertyName.UNLOADED_AMPHIBIOUS)
                .ifPresent(
                    property -> {
                      try {
                        property.setValue(true);
                      } catch (final MutableProperty.InvalidValueException e) {
                        // should not happen
                      }
                    }));
    return CasualtyOrderOfLosses.Parameters.builder()
        .targetsToPickFrom(amphibUnits)
        .player(testData.british)
        .combatValue(
            CombatValueBuilder.mainCombatValue()
                .enemyUnits(List.of())
                .friendlyUnits(amphibUnits)
                .side(BattleState.Side.OFFENSE)
                .gameSequence(testData.gameData.getSequence())
                .supportAttachments(testData.gameData.getUnitTypeList().getSupportRules())
                .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(testData.gameData.getProperties()))
                .gameDiceSides(testData.gameData.getDiceSides())
                .territoryEffects(List.of())
                .build())
        .battlesite(testData.france)
        .costs(testData.costMap)
        .data(testData.gameData)
        .build();
  }

  @Test
  void amphibAssaultWithoutImprovedArtillery() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(testData.tank(1));
    attackingUnits.addAll(testData.artillery(1));
    attackingUnits.addAll(testData.marine(1));
    attackingUnits.addAll(testData.marine(1));

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(amphibAssault(attackingUnits));

    assertThat(result, hasSize(4));
    assertThat(result.get(0).getType(), is(testData.artillery));
    assertThat(result.get(1).getType(), is(testData.tank));
    assertThat(result.get(2).getType(), is(testData.marine));
    assertThat(result.get(3).getType(), is(testData.marine)); // << bug, should be tank
  }

  @Test
  @DisplayName("Amphib assaulting marine should be taken last when it is strongest unit")
  void amphibAssaultIsTakenIntoAccount() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(testData.infantry(1));
    attackingUnits.addAll(testData.marine(1));
    attackingUnits.addAll(testData.artillery(1));

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(amphibAssault(attackingUnits));

    assertThat(result, hasSize(3));
    assertThat(result.get(0).getType(), is(testData.infantry));
    assertThat(result.get(1).getType(), is(testData.artillery));
    assertThat(
        "The marine is attacking at a 3 without support, it is the strongest land unit",
        result.get(2).getType(),
        is(testData.marine));
  }

  @Test
  @DisplayName("Tie between amphib marine and fighter goes to fighter")
  void favorStrongestAttackThenStrongestTotalPower() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(testData.marine(1));
    attackingUnits.addAll(testData.fighter(1));

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(amphibAssault(attackingUnits));

    assertThat(result, hasSize(2));
    assertThat(
        "marine is attacking at a 3, defends at 2, "
            + "ties with fighter but the weaker defense means it is chosen first",
        result.get(0).getType(),
        is(testData.marine));
    assertThat(
        "fighter ties with marine, attacking at 3, but fighter has better defense power of 4",
        result.get(1).getType(),
        is(testData.fighter));
  }

  @Test
  void strongestPowerOrdering() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(testData.infantry(1)); // attacks at 1
    attackingUnits.addAll(testData.fighter(1)); // attacks at 3
    attackingUnits.addAll(testData.bomber(1)); // attacks at 4

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(amphibAssault(attackingUnits));

    assertThat(result, hasSize(3));
    assertThat(result.get(0).getType(), is(testData.infantry));
    assertThat(result.get(1).getType(), is(testData.fighter));
    assertThat(result.get(2).getType(), is(testData.bomber));
  }

  @Test
  void infantryAndArtillery() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(testData.infantry(1)); // attacks at 2
    attackingUnits.addAll(testData.artillery(1)); // attacks at 2

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(amphibAssault(attackingUnits));

    assertThat(result, hasSize(2));
    assertThat(result.get(0).getType(), is(testData.infantry));
    assertThat(
        "Artillery has the better total power", result.get(1).getType(), is(testData.artillery));
  }

  @Test
  void nonAmphibiousMarineWithAmphibiousAssault() {
    testData.addTech(new ImprovedArtillerySupportAdvance(testData.gameData));

    final List<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(testData.tank(1));
    attackingUnits.addAll(testData.artillery(1));
    attackingUnits.addAll(testData.marine(1));

    final List<Unit> amphibMarines = new ArrayList<>(testData.marine(1));
    amphibMarines
        .get(0)
        .getProperty(Unit.PropertyName.UNLOADED_AMPHIBIOUS)
        .ifPresent(
            property -> {
              try {
                property.setValue(true);
              } catch (final MutableProperty.InvalidValueException e) {
                // should not happen
              }
            });
    attackingUnits.addAll(amphibMarines);

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(
            CasualtyOrderOfLosses.Parameters.builder()
                .targetsToPickFrom(attackingUnits)
                .player(testData.british)
                .combatValue(
                    CombatValueBuilder.mainCombatValue()
                        .enemyUnits(List.of())
                        .friendlyUnits(attackingUnits)
                        .side(BattleState.Side.OFFENSE)
                        .gameSequence(testData.gameData.getSequence())
                        .supportAttachments(testData.gameData.getUnitTypeList().getSupportRules())
                        .lhtrHeavyBombers(
                            Properties.getLhtrHeavyBombers(testData.gameData.getProperties()))
                        .gameDiceSides(testData.gameData.getDiceSides())
                        .territoryEffects(List.of())
                        .build())
                .battlesite(testData.france)
                .costs(testData.costMap)
                .data(testData.gameData)
                .build());

    assertThat(result, hasSize(4));
    assertThat(
        "Non amphibious marine only has attack of 2 since it doesn't get marine bonus",
        result.get(0),
        is(attackingUnits.get(2)));
    assertThat(result.get(1), is(attackingUnits.get(1)));
    assertThat("Amphibious marine has attack of 3", result.get(2), is(attackingUnits.get(3)));
    assertThat(result.get(3), is(attackingUnits.get(0)));
  }

  @Test
  void amphibiousAndNonAmphibiousCaching() {
    testData.addTech(new ImprovedArtillerySupportAdvance(testData.gameData));

    final List<Unit> amphibUnits = new ArrayList<>();
    amphibUnits.addAll(testData.tank(1));
    amphibUnits.addAll(testData.artillery(1));
    amphibUnits.addAll(testData.marine(1));

    amphibUnits.forEach(
        unit ->
            unit.getProperty(Unit.PropertyName.UNLOADED_AMPHIBIOUS)
                .ifPresent(
                    property -> {
                      try {
                        property.setValue(true);
                      } catch (final MutableProperty.InvalidValueException e) {
                        // should not happen
                      }
                    }));

    final List<Unit> attackingUnits = new ArrayList<>(amphibUnits);
    attackingUnits.addAll(testData.marine(1));

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(
            CasualtyOrderOfLosses.Parameters.builder()
                .targetsToPickFrom(attackingUnits)
                .player(testData.british)
                .combatValue(
                    CombatValueBuilder.mainCombatValue()
                        .enemyUnits(List.of())
                        .friendlyUnits(attackingUnits)
                        .side(BattleState.Side.OFFENSE)
                        .gameSequence(testData.gameData.getSequence())
                        .supportAttachments(testData.gameData.getUnitTypeList().getSupportRules())
                        .lhtrHeavyBombers(
                            Properties.getLhtrHeavyBombers(testData.gameData.getProperties()))
                        .gameDiceSides(testData.gameData.getDiceSides())
                        .territoryEffects(List.of())
                        .build())
                .battlesite(testData.france)
                .costs(testData.costMap)
                .data(testData.gameData)
                .build());

    assertThat(
        "Non amphibious marine only has attack of 2 since it doesn't get marine bonus",
        result.get(0),
        is(attackingUnits.get(3)));
    assertThat("Amphibious marine has attack of 3", result.get(2), is(attackingUnits.get(2)));

    final List<Unit> result2 =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(
            CasualtyOrderOfLosses.Parameters.builder()
                .targetsToPickFrom(attackingUnits.subList(0, 3))
                .player(testData.british)
                .combatValue(
                    CombatValueBuilder.mainCombatValue()
                        .enemyUnits(List.of())
                        .friendlyUnits(attackingUnits.subList(0, 3))
                        .side(BattleState.Side.OFFENSE)
                        .gameSequence(testData.gameData.getSequence())
                        .supportAttachments(testData.gameData.getUnitTypeList().getSupportRules())
                        .lhtrHeavyBombers(
                            Properties.getLhtrHeavyBombers(testData.gameData.getProperties()))
                        .gameDiceSides(testData.gameData.getDiceSides())
                        .territoryEffects(List.of())
                        .build())
                .battlesite(testData.france)
                .costs(testData.costMap)
                .data(testData.gameData)
                .build());

    assertThat(result2, hasSize(3));
    assertThat(result2.get(0), is(attackingUnits.get(1)));
    assertThat("Amphibious marine has attack of 3", result2.get(1), is(attackingUnits.get(2)));
    assertThat(result2.get(2), is(attackingUnits.get(0)));
  }
}
