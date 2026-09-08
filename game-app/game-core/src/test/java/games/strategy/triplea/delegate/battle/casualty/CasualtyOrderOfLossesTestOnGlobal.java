package games.strategy.triplea.delegate.battle.casualty;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.HeavyBomberAdvance;
import games.strategy.triplea.delegate.ImprovedArtillerySupportAdvance;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.experimental.UtilityClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.triplea.java.collections.IntegerMap;

@SuppressWarnings("SameParameterValue")
class CasualtyOrderOfLossesTestOnGlobal {
  private static final GameData data = TestMapGameData.GLOBAL1940.getGameData();
  private static final GamePlayer BRITISH =
      checkNotNull(data.getPlayerList().getPlayerId("British"));
  private static final Territory FRANCE = checkNotNull(territory("France", data));
  private static final UnitType INFANTRY =
      checkNotNull(data.getUnitTypeList().getUnitTypeOrThrow("infantry"));
  private static final UnitType TANK =
      checkNotNull(data.getUnitTypeList().getUnitTypeOrThrow("armour"));
  private static final UnitType MARINE =
      checkNotNull(data.getUnitTypeList().getUnitTypeOrThrow("marine"));
  private static final UnitType ARTILLERY =
      checkNotNull(data.getUnitTypeList().getUnitTypeOrThrow("artillery"));
  private static final UnitType SUBMARINE =
      checkNotNull(data.getUnitTypeList().getUnitTypeOrThrow("submarine"));
  private static final UnitType DESTROYER =
      checkNotNull(data.getUnitTypeList().getUnitTypeOrThrow("destroyer"));
  private static final UnitType CARRIER =
      checkNotNull(data.getUnitTypeList().getUnitTypeOrThrow("carrier"));
  private static final UnitType BOMBER =
      checkNotNull(data.getUnitTypeList().getUnitTypeOrThrow("bomber"));
  private static final UnitType BATTLESHIP =
      checkNotNull(data.getUnitTypeList().getUnitTypeOrThrow("battleship"));

  private static final IntegerMap<UnitType> COST_MAP =
      IntegerMap.of(
          Map.of(
              INFANTRY, 3,
              TANK, 5,
              MARINE, 4,
              ARTILLERY, 4,
              SUBMARINE, 6,
              DESTROYER, 8,
              CARRIER, 16,
              BOMBER, 12,
              BATTLESHIP, 20));

  @UtilityClass
  static class DataFactory {
    static Collection<Unit> britishInfantry(final int count) {
      return createUnit(INFANTRY, count);
    }

    static Collection<Unit> britishTank(final int count) {
      return createUnit(TANK, count);
    }

    static Collection<Unit> britishMarine(final int count) {
      return createUnit(MARINE, count);
    }

    static Collection<Unit> britishArtillery(final int count) {
      return createUnit(ARTILLERY, count);
    }

    static Collection<Unit> britishCarrier(final int count) {
      return createUnit(CARRIER, count);
    }

    static Collection<Unit> britishDestroyer(final int count) {
      return createUnit(DESTROYER, count);
    }

    static Collection<Unit> britishSubmarine(final int count) {
      return createUnit(SUBMARINE, count);
    }

    static Collection<Unit> britishBattleship(final int count) {
      return createUnit(BATTLESHIP, count);
    }

    static Collection<Unit> britishBomber(final int count) {
      return createUnit(BOMBER, count);
    }

    private static Collection<Unit> createUnit(final UnitType unitType, final int count) {
      return IntStream.range(0, count)
          .mapToObj(i -> new Unit(unitType, BRITISH, data))
          .collect(Collectors.toSet());
    }
  }

  @BeforeEach
  void clearCache() {
    CasualtyOrderOfLosses.clearOolCache();
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 10})
  void simpleCaseWithAllInfantry(final int infantryCount) {
    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(
            attackingWith(DataFactory.britishInfantry(infantryCount)));

    assertThat(result).hasSize(infantryCount);
  }

  private CasualtyOrderOfLosses.Parameters attackingWith(final Collection<Unit> units) {
    return CasualtyOrderOfLosses.Parameters.builder()
        .targetsToPickFrom(units)
        .player(BRITISH)
        .combatValue(
            CombatValueBuilder.mainCombatValue()
                .enemyUnits(List.of())
                .friendlyUnits(units)
                .side(BattleState.Side.OFFENSE)
                .gameSequence(data.getSequence())
                .supportAttachments(data.getUnitTypeList().getSupportRules())
                .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(data.getProperties()))
                .gameDiceSides(data.getDiceSides())
                .territoryEffects(List.of())
                .build())
        .battlesite(FRANCE)
        .costs(COST_MAP)
        .data(data)
        .build();
  }

  @Test
  void infantryAndTank() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(DataFactory.britishInfantry(1));
    attackingUnits.addAll(DataFactory.britishTank(1));

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(attackingWith(attackingUnits));

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getType())
        .as("infantry has a weaker attack and should be removed first")
        .isEqualTo(INFANTRY);
    assertThat(result.get(1).getType()).isEqualTo(TANK);
  }

  @Test
  void marineInfantryArtilleryAndTank() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(DataFactory.britishInfantry(1));
    attackingUnits.addAll(DataFactory.britishTank(1));
    attackingUnits.addAll(DataFactory.britishMarine(1));
    attackingUnits.addAll(DataFactory.britishArtillery(1));

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(attackingWith(attackingUnits));

    assertThat(result).hasSize(4);

    // Note: It's a bug we take marine first, artillery can support the marine instead
    // of infantry, would be better to kill infantry first as it is lower cost
    assertThat(result.get(0).getType()).isEqualTo(MARINE);
    assertThat(result.get(1).getType()).isEqualTo(INFANTRY);
    assertThat(result.get(2).getType()).isEqualTo(ARTILLERY);
    assertThat(result.get(3).getType()).isEqualTo(TANK);
  }

  @Test
  @DisplayName("Verify that amphib assaulting marine is given higher precedence over infantry")
  void infantryBeforeAmphibAssaultingMarines() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(DataFactory.britishInfantry(2));
    attackingUnits.addAll(DataFactory.britishMarine(2));
    attackingUnits.addAll(DataFactory.britishArtillery(2));

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(amphibAssault(attackingUnits));

    assertThat(result).hasSize(6);
    assertThat(result.get(0).getType()).isEqualTo(INFANTRY);
    assertThat(result.get(1).getType()).isEqualTo(INFANTRY);
    assertThat(result.get(2).getType()).isEqualTo(ARTILLERY);
    assertThat(result.get(3).getType()).isEqualTo(ARTILLERY);
    assertThat(result.get(4).getType()).isEqualTo(MARINE);
    assertThat(result.get(5).getType()).isEqualTo(MARINE);
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
        .player(BRITISH)
        .combatValue(
            CombatValueBuilder.mainCombatValue()
                .enemyUnits(List.of())
                .friendlyUnits(amphibUnits)
                .side(BattleState.Side.OFFENSE)
                .gameSequence(data.getSequence())
                .supportAttachments(data.getUnitTypeList().getSupportRules())
                .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(data.getProperties()))
                .gameDiceSides(data.getDiceSides())
                .territoryEffects(List.of())
                .build())
        .battlesite(FRANCE)
        .costs(COST_MAP)
        .data(data)
        .build();
  }

  @Test
  void marinesAndArtillery() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(DataFactory.britishMarine(3));
    attackingUnits.addAll(DataFactory.britishArtillery(3));

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(amphibAssault(attackingUnits));

    assertThat(result).hasSize(6);
    assertThat(result.get(0).getType()).isEqualTo(ARTILLERY);
    assertThat(result.get(1).getType()).isEqualTo(ARTILLERY);
    assertThat(result.get(2).getType()).isEqualTo(ARTILLERY);
    assertThat(result.get(3).getType()).isEqualTo(MARINE);
    assertThat(result.get(4).getType()).isEqualTo(MARINE);
    assertThat(result.get(5).getType()).isEqualTo(MARINE);
  }

  @Test
  void threeMarinesAndTwoArtillery() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(DataFactory.britishMarine(3));
    attackingUnits.addAll(DataFactory.britishArtillery(2));

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(amphibAssault(attackingUnits));

    assertThat(result).hasSize(5);
    assertThat(result.get(0).getType())
        .as("First artillery is not providing support, power of 2")
        .isEqualTo(ARTILLERY);
    assertThat(result.get(4).getType())
        .as("Marine must be the last to be chosen")
        .isEqualTo(MARINE);
  }

  @Test
  void navalOrderingOnAttack() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(DataFactory.britishSubmarine(1));
    attackingUnits.addAll(DataFactory.britishDestroyer(1));
    attackingUnits.addAll(DataFactory.britishCarrier(1));

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(attackingWith(attackingUnits));

    assertThat(result).hasSize(3);
    assertThat(result.get(0).getType()).isEqualTo(CARRIER);
    assertThat(result.get(1).getType()).isEqualTo(SUBMARINE);
    assertThat(result.get(2).getType()).isEqualTo(DESTROYER);
  }

  @Test
  void navalOrderingOnDefense() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(DataFactory.britishSubmarine(1));
    attackingUnits.addAll(DataFactory.britishDestroyer(1));
    attackingUnits.addAll(DataFactory.britishCarrier(1));

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(defendingWith(attackingUnits));

    assertThat(result).hasSize(3);
    assertThat(result.get(0).getType()).isEqualTo(SUBMARINE);
    assertThat(result.get(1).getType()).isEqualTo(DESTROYER);
    assertThat(result.get(2).getType()).isEqualTo(CARRIER);
  }

  private CasualtyOrderOfLosses.Parameters defendingWith(final Collection<Unit> units) {
    return CasualtyOrderOfLosses.Parameters.builder()
        .targetsToPickFrom(units)
        .player(BRITISH)
        .combatValue(
            CombatValueBuilder.mainCombatValue()
                .enemyUnits(List.of())
                .friendlyUnits(units)
                .side(BattleState.Side.DEFENSE)
                .gameSequence(data.getSequence())
                .supportAttachments(data.getUnitTypeList().getSupportRules())
                .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(data.getProperties()))
                .gameDiceSides(data.getDiceSides())
                .territoryEffects(List.of())
                .build())
        .battlesite(FRANCE)
        .costs(COST_MAP)
        .data(data)
        .build();
  }

  @Test
  @DisplayName("Heavy bomber has a higher attack power than bship, we select bship first")
  void heavyBomberAndBattleship() {
    givenHeavyBombers();

    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(DataFactory.britishBomber(1));
    attackingUnits.addAll(DataFactory.britishBattleship(1));

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(attackingWith(attackingUnits));

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getType()).isEqualTo(BATTLESHIP);
    assertThat(result.get(1).getType()).isEqualTo(BOMBER);
  }

  private void givenHeavyBombers() {
    addTech(new HeavyBomberAdvance(data));
  }

  private void addTech(final TechAdvance techAdvance) {
    final var change =
        ChangeFactory.attachmentPropertyChange(
            BRITISH.getTechAttachment(), true, techAdvance.getProperty());
    data.performChange(change);
  }

  @Test
  @DisplayName(
      "bomber has an equal attack power as bship, select bomber first as it is less expensive")
  void bomberAndBattleship() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(DataFactory.britishBomber(1));
    attackingUnits.addAll(DataFactory.britishBattleship(1));

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(attackingWith(attackingUnits));

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getType()).isEqualTo(BOMBER);
    assertThat(result.get(1).getType()).isEqualTo(BATTLESHIP);
  }

  @Test
  void improvedArtillery() {
    addTech(new ImprovedArtillerySupportAdvance(data));

    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(DataFactory.britishTank(1));
    attackingUnits.addAll(DataFactory.britishArtillery(1));
    attackingUnits.addAll(DataFactory.britishMarine(1));
    attackingUnits.addAll(DataFactory.britishMarine(1));

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(amphibAssault(attackingUnits));

    assertThat(result).hasSize(4);
    assertThat(result.get(0).getType()).isEqualTo(MARINE);
    assertThat(result.get(1).getType()).isEqualTo(MARINE);
    assertThat(result.get(2).getType()).isEqualTo(ARTILLERY);
    assertThat(result.get(3).getType()).isEqualTo(TANK); // attack at 3
  }
}
