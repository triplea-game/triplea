package games.strategy.triplea.odds.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.battle.BattleResults;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

/**
 * The statistics of many battle simulation runs, queried by the panel and the AI: win/draw
 * probabilities, average survivors and TUV, and rounds fought.
 *
 * <p>Two implementations satisfy this: the old-path {@link ListBackedAggregateResults}, which
 * derives every stat from per-run {@link BattleResults}, and the bounded-context calc's
 * count-backed results, which compute straight from survivor counts. Only the list-backed one
 * exposes {@link #getResults()}; on any other implementation it throws.
 */
public abstract class AggregateResults {
  @Getter @Setter private long time;

  /**
   * The raw per-run results. Only the list-backed implementation carries them; the count-backed
   * calc discards per-run units by design (its stats come from counts), so it does not offer this.
   */
  public List<BattleResults> getResults() {
    throw new UnsupportedOperationException(
        "getResults is available only on the list-backed implementation");
  }

  public abstract double getAttackerWinPercent();

  public abstract double getDefenderWinPercent();

  public abstract double getDrawPercent();

  public abstract double getAverageAttackingUnitsLeft();

  public abstract double getAverageAttackingUnitsLeftWhenAttackerWon();

  public abstract double getAverageDefendingUnitsLeft();

  public abstract double getAverageDefendingUnitsLeftWhenDefenderWon();

  public abstract double getAverageBattleRoundsFought();

  public abstract Tuple<Double, Double> getAverageTuvOfUnitsLeftOver(
      IntegerMap<UnitType> attackerCostsForTuv, IntegerMap<UnitType> defenderCostsForTuv);

  public abstract double getAverageTuvSwing(
      GamePlayer attacker,
      Collection<Unit> attackers,
      GamePlayer defender,
      Collection<Unit> defenders,
      GameData data);

  public abstract int getRollCount();

  public abstract Collection<Unit> getAverageAttackingUnitsRemaining();

  public abstract Collection<Unit> getAverageDefendingUnitsRemaining();
}
