package org.triplea.ai.benchmark.runner;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.attachments.TerritoryAttachment;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.triplea.ai.benchmark.metrics.Metrics;

/**
 * The early-end rule: an alliance has clearly won once it leads at {@link #ROUNDS} round ends in a
 * row, so the rest of the game only costs run time. Leading means more unit value (by {@link
 * #TUV_LEAD}) and more income than everyone outside it, all its own capitals held, and at least one
 * enemy capital taken. The thresholds are constants shared by every kata, so results stay
 * comparable; tune them here.
 */
final class Decisive {
  static final int ROUNDS = 2;
  static final double TUV_LEAD = 1.10;

  private String current;
  private int streak;

  /** One alliance measured against every player outside it. */
  record Standing(
      double tuv,
      double enemyTuv,
      double income,
      double enemyIncome,
      boolean holdsAllOwnCapitals,
      boolean holdsEnemyCapital) {
    boolean leads() {
      return tuv > enemyTuv * TUV_LEAD
          && income > enemyIncome
          && holdsAllOwnCapitals
          && holdsEnemyCapital;
    }
  }

  /** The alliance that leads right now, or null. */
  @Nullable
  static String leader(final GameData data) {
    for (final String alliance : data.getAllianceTracker().getAlliances()) {
      if (standing(data, alliance).leads()) {
        return alliance;
      }
    }
    return null;
  }

  /**
   * Notes who leads at a round end; returns the alliance once it has led for {@link #ROUNDS} round
   * ends in a row, else null.
   */
  @Nullable
  String observe(@Nullable final String leader) {
    if (leader == null) {
      current = null;
      streak = 0;
      return null;
    }
    streak = leader.equals(current) ? streak + 1 : 1;
    current = leader;
    return streak >= ROUNDS ? current : null;
  }

  private static Standing standing(final GameData data, final String alliance) {
    final var sides = BenchmarkMain.Sides.of(data, alliance);
    final Set<GamePlayer> own = Set.copyOf(sides.seat());
    return new Standing(
        sum("tuv", data, sides.seat()),
        sum("tuv", data, sides.enemy()),
        sum("income", data, sides.seat()),
        sum("income", data, sides.enemy()),
        sides.seat().stream().allMatch(p -> capitalsOwnedBy(data, p, own, true)),
        sides.enemy().stream()
            .anyMatch(p -> !capitals(data, p).isEmpty() && capitalsOwnedBy(data, p, own, false)));
  }

  /**
   * Whether the capitals of {@code player} are held by {@code holders}: all of them when {@code
   * all}, else any one.
   */
  private static boolean capitalsOwnedBy(
      final GameData data,
      final GamePlayer player,
      final Set<GamePlayer> holders,
      final boolean all) {
    final var held = capitals(data, player).stream().map(t -> holders.contains(t.getOwner()));
    return all ? held.allMatch(Boolean::booleanValue) : held.anyMatch(Boolean::booleanValue);
  }

  private static List<Territory> capitals(final GameData data, final GamePlayer player) {
    return TerritoryAttachment.getAllCapitals(player, data.getMap());
  }

  private static double sum(
      final String metric, final GameData data, final Collection<GamePlayer> players) {
    return Metrics.byId(metric).value(data, players);
  }
}
