package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.IBattle.BattleDomain;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** Common domain partitioning and scheduling policy for separated air and ground combat. */
public final class AirGroundBattlePolicy {
  public static final String SEPARATE_AIR_AND_GROUND_COMBAT = "Separate Air And Ground Combat";

  private AirGroundBattlePolicy() {}

  public static boolean isSeparatedCombatEnabled(final GameState gameState) {
    return gameState != null
        && gameState.getProperties().get(SEPARATE_AIR_AND_GROUND_COMBAT, false);
  }

  public static List<Unit> unitsForDomain(final Collection<Unit> units, final BattleDomain domain) {
    return switch (domain) {
      case AIR -> units.stream().filter(Matches.unitIsAir()).toList();
      case GROUND -> units.stream().filter(Matches.unitIsNotAir()).toList();
      case RAID -> List.copyOf(units);
    };
  }

  public static List<BattleType> orderForResolution(final Collection<BattleType> battleTypes) {
    return battleTypes.stream()
        .sorted(Comparator.comparingInt(AirGroundBattlePolicy::priority))
        .toList();
  }

  public static boolean mustPrecede(final BattleType blocking, final BattleType blocked) {
    return priority(blocking) < priority(blocked);
  }

  private static int priority(final BattleType battleType) {
    return switch (battleType.getDomain()) {
      case RAID -> 0;
      case AIR -> 1;
      case GROUND -> 2;
    };
  }
}
