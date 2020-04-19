package games.strategy.triplea.delegate.battle.casualty;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.experimental.UtilityClass;
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
      checkNotNull(data.getUnitTypeList().getUnitType("infantry"));
  private static final UnitType TANK = checkNotNull(data.getUnitTypeList().getUnitType("armour"));
  private static final UnitType MARINE = checkNotNull(data.getUnitTypeList().getUnitType("marine"));
  private static final UnitType ARTILLERY =
      checkNotNull(data.getUnitTypeList().getUnitType("artillery"));
  private static final IntegerMap<UnitType> COST_MAP =
      IntegerMap.of(Map.of(INFANTRY, 3, TANK, 5, MARINE, 4, ARTILLERY, 4));

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

    private static Collection<Unit> createUnit(final UnitType unitType, final int count) {
      return IntStream.range(0, count)
          .mapToObj(i -> new Unit(unitType, BRITISH, data))
          .collect(Collectors.toSet());
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 10})
  void simpleCaseWithAllInfantry(final int infantryCount) {
    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(
            attackingWith(DataFactory.britishInfantry(infantryCount)));

    assertThat(result, hasSize(infantryCount));
  }

  private CasualtyOrderOfLosses.Parameters attackingWith(final Collection<Unit> units) {
    return CasualtyOrderOfLosses.Parameters.builder()
        .targetsToPickFrom(units)
        .defending(false)
        .player(BRITISH)
        .enemyUnits(List.of()) // << TODO: remove this parameter should not matter
        .amphibious(false)
        .amphibiousLandAttackers(List.of())
        .battlesite(FRANCE)
        .costs(COST_MAP)
        .territoryEffects(List.of())
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

    assertThat(result, hasSize(2));
    assertThat(
        "infantry has a weaker attack and should be removed first",
        result.get(0).getType(),
        is(INFANTRY));
    assertThat(result.get(1).getType(), is(TANK));
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

    assertThat(result, hasSize(4));

    // Note: It's a bug we take marine first, artillery can support the marine instead
    // of infantry, would be better to kill infantry first as it is lower cost
    assertThat(result.get(0).getType(), is(MARINE));
    assertThat(result.get(1).getType(), is(INFANTRY));
    assertThat(result.get(2).getType(), is(ARTILLERY));
    assertThat(result.get(3).getType(), is(TANK));
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

    assertThat(result, hasSize(6));
    assertThat(result.get(0).getType(), is(INFANTRY));
    assertThat(result.get(1).getType(), is(INFANTRY));
    assertThat(result.get(2).getType(), is(MARINE));
    assertThat(result.get(3).getType(), is(MARINE));
    assertThat(result.get(4).getType(), is(ARTILLERY));
    assertThat(result.get(5).getType(), is(ARTILLERY));
  }

  private CasualtyOrderOfLosses.Parameters amphibAssault(final Collection<Unit> amphibUnits) {
    return CasualtyOrderOfLosses.Parameters.builder()
        .targetsToPickFrom(amphibUnits)
        .defending(false)
        .player(BRITISH)
        .enemyUnits(List.of()) // << TODO: remove this parameter should not matter
        .amphibious(true)
        .amphibiousLandAttackers(amphibUnits)
        .battlesite(FRANCE)
        .costs(COST_MAP)
        .territoryEffects(List.of())
        .data(data)
        .build();
  }

  @Test
  @DisplayName("Verify that amphib assualting marines and artillery are interleaved")
  void interleaveArtilleryAndMarines() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(DataFactory.britishMarine(3));
    attackingUnits.addAll(DataFactory.britishArtillery(3));

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(amphibAssault(attackingUnits));

    assertThat(result, hasSize(6));
    assertThat(result.get(0).getType(), is(MARINE));
    assertThat(result.get(1).getType(), is(MARINE));
    assertThat(result.get(2).getType(), is(MARINE));
    assertThat(result.get(3).getType(), is(ARTILLERY));
    assertThat(result.get(4).getType(), is(ARTILLERY));
    assertThat(result.get(5).getType(), is(ARTILLERY));
  }
}
