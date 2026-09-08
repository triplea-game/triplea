package games.strategy.triplea.delegate.battle.casualty;

import static org.assertj.core.api.Assertions.assertThat;

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

    assertThat(result).hasSize(4);
    assertThat(result.get(0).getType()).isEqualTo(testData.tank);
    assertThat(result.get(1).getType()).isEqualTo(testData.artillery);
    assertThat(result.get(2).getType()).isEqualTo(testData.marine);
    assertThat(result.get(3).getType()).isEqualTo(testData.marine);
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

    assertThat(result).hasSize(4);
    assertThat(result.get(0).getType()).isEqualTo(testData.artillery);
    assertThat(result.get(1).getType()).isEqualTo(testData.tank);
    assertThat(result.get(2).getType()).isEqualTo(testData.marine);
    assertThat(result.get(3).getType()).isEqualTo(testData.marine); // << bug, should be tank
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

    assertThat(result).hasSize(3);
    assertThat(result.get(0).getType()).isEqualTo(testData.infantry);
    assertThat(result.get(1).getType()).isEqualTo(testData.artillery);
    assertThat(result.get(2).getType())
        .as("The marine is attacking at a 3 without support, it is the strongest land unit")
        .isEqualTo(testData.marine);
  }

  @Test
  @DisplayName("Tie between amphib marine and fighter goes to fighter")
  void favorStrongestAttackThenStrongestTotalPower() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(testData.marine(1));
    attackingUnits.addAll(testData.fighter(1));

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(amphibAssault(attackingUnits));

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getType())
        .as(
            "marine is attacking at a 3, defends at 2, "
                + "ties with fighter but the weaker defense means it is chosen first")
        .isEqualTo(testData.marine);
    assertThat(result.get(1).getType())
        .as("fighter ties with marine, attacking at 3, but fighter has better defense power of 4")
        .isEqualTo(testData.fighter);
  }

  @Test
  void strongestPowerOrdering() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(testData.infantry(1)); // attacks at 1
    attackingUnits.addAll(testData.fighter(1)); // attacks at 3
    attackingUnits.addAll(testData.bomber(1)); // attacks at 4

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(amphibAssault(attackingUnits));

    assertThat(result).hasSize(3);
    assertThat(result.get(0).getType()).isEqualTo(testData.infantry);
    assertThat(result.get(1).getType()).isEqualTo(testData.fighter);
    assertThat(result.get(2).getType()).isEqualTo(testData.bomber);
  }

  @Test
  void infantryAndArtillery() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(testData.infantry(1)); // attacks at 2
    attackingUnits.addAll(testData.artillery(1)); // attacks at 2

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(amphibAssault(attackingUnits));

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getType()).isEqualTo(testData.infantry);
    assertThat(result.get(1).getType())
        .as("Artillery has the better total power")
        .isEqualTo(testData.artillery);
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

    assertThat(result).hasSize(4);
    assertThat(result.get(0))
        .as("Non amphibious marine only has attack of 2 since it doesn't get marine bonus")
        .isEqualTo(attackingUnits.get(2));
    assertThat(result.get(1)).isEqualTo(attackingUnits.get(1));
    assertThat(result.get(2))
        .as("Amphibious marine has attack of 3")
        .isEqualTo(attackingUnits.get(3));
    assertThat(result.get(3)).isEqualTo(attackingUnits.get(0));
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

    assertThat(result.get(0))
        .as("Non amphibious marine only has attack of 2 since it doesn't get marine bonus")
        .isEqualTo(attackingUnits.get(3));
    assertThat(result.get(2))
        .as("Amphibious marine has attack of 3")
        .isEqualTo(attackingUnits.get(2));

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

    assertThat(result2).hasSize(3);
    assertThat(result2.get(0)).isEqualTo(attackingUnits.get(1));
    assertThat(result2.get(1))
        .as("Amphibious marine has attack of 3")
        .isEqualTo(attackingUnits.get(2));
    assertThat(result2.get(2)).isEqualTo(attackingUnits.get(0));
  }
}
