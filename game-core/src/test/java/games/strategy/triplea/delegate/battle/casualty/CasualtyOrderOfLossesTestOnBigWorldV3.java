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
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.delegate.ImprovedArtillerySupportAdvance;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.battle.UnitBattleComparator.CombatModifiers;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.experimental.UtilityClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.IntegerMap;

@SuppressWarnings("SameParameterValue")
class CasualtyOrderOfLossesTestOnBigWorldV3 {
  private static final GameData data = TestMapGameData.BIG_WORLD_1942_V3.getGameData();
  private static final GamePlayer BRITISH =
      checkNotNull(data.getPlayerList().getPlayerId("British"));
  private static final Territory FRANCE = checkNotNull(territory("France", data));
  private static final UnitType TANK = checkNotNull(data.getUnitTypeList().getUnitType("armour"));
  private static final UnitType MARINE = checkNotNull(data.getUnitTypeList().getUnitType("marine"));
  private static final UnitType ARTILLERY =
      checkNotNull(data.getUnitTypeList().getUnitType("artillery"));

  private static final IntegerMap<UnitType> COST_MAP =
      IntegerMap.of(
          Map.of(
              MARINE, 4,
              ARTILLERY, 4));

  @UtilityClass
  static class DataFactory {
    Collection<Unit> britishTank(final int count) {
      return createUnit(TANK, count);
    }

    Collection<Unit> britishMarine(final int count) {
      return createUnit(MARINE, count);
    }

    Collection<Unit> britishArtillery(final int count) {
      return createUnit(ARTILLERY, count);
    }

    private Collection<Unit> createUnit(final UnitType unitType, final int count) {
      return IntStream.range(0, count)
          .mapToObj(i -> new Unit(unitType, BRITISH, data))
          .collect(Collectors.toSet());
    }
  }

  @BeforeEach
  void clearCache() {
    CasualtyOrderOfLosses.clearOolCache();
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

    assertThat(result, hasSize(4));
    assertThat(result.get(0).getType(), is(TANK));
    assertThat(result.get(1).getType(), is(ARTILLERY));
    assertThat(result.get(2).getType(), is(MARINE));
    assertThat(result.get(3).getType(), is(MARINE));
  }

  private void addTech(final TechAdvance techAdvance) {
    final var change =
        ChangeFactory.attachmentPropertyChange(
            TechAttachment.get(BRITISH), true, techAdvance.getProperty());
    data.performChange(change);
  }

  private CasualtyOrderOfLosses.Parameters amphibAssault(final Collection<Unit> amphibUnits) {
    return CasualtyOrderOfLosses.Parameters.builder()
        .targetsToPickFrom(amphibUnits)
        .combatModifiers(
            CombatModifiers.builder()
                .defending(false)
                .territoryEffects(List.of())
                .amphibious(true)
                .build())
        .player(BRITISH)
        .enemyUnits(List.of())
        .amphibiousLandAttackers(amphibUnits)
        .battlesite(FRANCE)
        .costs(COST_MAP)
        .data(data)
        .build();
  }

  @Test
  void amphibAssaultWithoutImprovedArtillery() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(DataFactory.britishTank(1));
    attackingUnits.addAll(DataFactory.britishArtillery(1));
    attackingUnits.addAll(DataFactory.britishMarine(1));
    attackingUnits.addAll(DataFactory.britishMarine(1));

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(amphibAssault(attackingUnits));

    assertThat(result, hasSize(4));
    assertThat(result.get(0).getType(), is(TANK)); // << bug, should be marine or artillery first
    assertThat(result.get(1).getType(), is(ARTILLERY));
    assertThat(result.get(2).getType(), is(MARINE));
    assertThat(result.get(3).getType(), is(MARINE)); // << bug, should be tank
  }
}
