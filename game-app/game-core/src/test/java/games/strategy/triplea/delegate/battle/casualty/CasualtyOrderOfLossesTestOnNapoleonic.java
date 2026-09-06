package games.strategy.triplea.delegate.battle.casualty;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
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
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.IntegerMap;

@SuppressWarnings("SameParameterValue")
class CasualtyOrderOfLossesTestOnNapoleonic {
  private static final GameData data = TestMapGameData.NAPOLEONIC_EMPIRES.getGameData();
  private static final GamePlayer BRITISH =
      checkNotNull(data.getPlayerList().getPlayerId("UnitedKingdom"));
  private static final Territory NORMANDY = checkNotNull(territory("Normandy", data));
  private static final UnitType HOWITZER = // attacks at 1, gives support
      checkNotNull(data.getUnitTypeList().getUnitTypeOrThrow("Howitzer"));
  private static final UnitType ARTILLERY = // attacks at 2, gives support
      checkNotNull(data.getUnitTypeList().getUnitTypeOrThrow("Artillery"));
  private static final UnitType FUSILIER = // attacks at 1, isSupportable
      checkNotNull(data.getUnitTypeList().getUnitTypeOrThrow("Fusiliers"));
  private static final UnitType GRENADIERS = // attacks at 3, isSupportable
      checkNotNull(data.getUnitTypeList().getUnitTypeOrThrow("Grenadiers"));

  private static final IntegerMap<UnitType> COST_MAP =
      IntegerMap.of(Map.of(FUSILIER, 4, HOWITZER, 4));

  @UtilityClass
  static class DataFactory {
    static Collection<Unit> howitzer(final int count) {
      return createUnit(HOWITZER, count);
    }

    static Collection<Unit> artillery(final int count) {
      return createUnit(ARTILLERY, count);
    }

    static Collection<Unit> fusilier(final int count) {
      return createUnit(FUSILIER, count);
    }

    static Collection<Unit> grenadiers(final int count) {
      return createUnit(GRENADIERS, count);
    }

    static Collection<Unit> createUnit(final UnitType unitType, final int count) {
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
  void twoHowitzerandOneFusilier() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(DataFactory.fusilier(1));
    attackingUnits.addAll(DataFactory.howitzer(2));

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(attackingWith(attackingUnits));

    assertThat(result).hasSize(3);
    assertThat(result.get(0).getType()).isEqualTo(HOWITZER);
    assertThat(result.get(1).getType()).isEqualTo(HOWITZER);
    assertThat(result.get(2).getType()).isEqualTo(FUSILIER);
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
        .battlesite(NORMANDY)
        .costs(COST_MAP)
        .data(data)
        .build();
  }

  @Test
  void twoHowitzerandTwoFusilier() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(DataFactory.fusilier(2));
    attackingUnits.addAll(DataFactory.howitzer(2));

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(attackingWith(attackingUnits));

    assertThat(result).hasSize(4);
    assertThat(result.get(0).getType()).isEqualTo(HOWITZER);
    assertThat(result.get(1).getType()).isEqualTo(HOWITZER);
    assertThat(result.get(2).getType()).isEqualTo(FUSILIER);
    assertThat(result.get(3).getType()).isEqualTo(FUSILIER);
  }

  @Test
  void complexArmy() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(DataFactory.howitzer(2));
    attackingUnits.addAll(DataFactory.artillery(2));
    attackingUnits.addAll(DataFactory.fusilier(2));
    attackingUnits.addAll(DataFactory.grenadiers(2));

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(attackingWith(attackingUnits));

    assertThat(result).hasSize(8);
    assertThat(result.get(0).getType()).isEqualTo(HOWITZER);
    assertThat(result.get(1).getType()).isEqualTo(HOWITZER);
    assertThat(result.get(2).getType()).isEqualTo(FUSILIER);
    assertThat(result.get(3).getType()).isEqualTo(FUSILIER);
    assertThat(result.get(4).getType()).isEqualTo(ARTILLERY);
    assertThat(result.get(5).getType()).isEqualTo(ARTILLERY);
    assertThat(result.get(6).getType()).isEqualTo(GRENADIERS);
    assertThat(result.get(7).getType()).isEqualTo(GRENADIERS);
  }
}
