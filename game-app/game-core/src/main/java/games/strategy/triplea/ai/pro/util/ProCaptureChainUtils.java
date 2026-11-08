package games.strategy.triplea.ai.pro.util;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.pro.data.ProBattleResult;
import games.strategy.triplea.ai.pro.data.ProOtherMoveOptions;
import games.strategy.triplea.ai.pro.data.ProTerritory;
import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.util.TuvUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

/**
 * Judges a capital or factory against the enemies' armies attacking one after another, not only the
 * strongest one. An army that loses still thins the defence for the next, and an attacker gladly
 * gives up an army that pins the defenders when the prize is a capital.
 */
@UtilityClass
public class ProCaptureChainUtils {
  /** Rounds of income an owner is assumed to lose before it can retake a captured territory. */
  public static final int RECAPTURE_HORIZON = 3;

  /** Share of a territory's income and factory loss counted for a factory that is no capital. */
  public static final double FACTORY_WEIGHT = 0.5;

  /** Chain capture chance in percent below which a territory is not worth defending further. */
  public static final double MIN_CAPTURE_PERCENTAGE = 25;

  /** Capture chance times loss value, in PUs, a territory must risk to be worth defending. */
  public static final double MIN_EXPECTED_LOSS = 10;

  /** One enemy player's attack on a territory. */
  public record Army(Collection<Unit> units, Collection<Unit> bombardUnits) {}

  /**
   * The outcome of the armies attacking in turn.
   *
   * @param capturePercentage chance in percent that some army captures
   * @param armyCapturePercentages each army's own chance, against the defenders it faces
   * @param defendersRemaining the defenders left on average when the last army is done
   * @param strongestAlonePercentage the best chance of any army against the starting defenders by
   *     itself, or 0 when the chain stays under {@link #MIN_CAPTURE_PERCENTAGE} and it wasn't run
   */
  public record Chain(
      double capturePercentage,
      List<Double> armyCapturePercentages,
      Collection<Unit> defendersRemaining,
      double strongestAlonePercentage) {}

  /** The armies that can attack {@code t}, in the enemies' turn order. */
  public static List<Army> armies(final ProOtherMoveOptions enemyAttackOptions, final Territory t) {
    final List<Army> armies = new ArrayList<>();
    for (final ProTerritory enemy : enemyAttackOptions.getAll(t)) {
      final Set<Unit> units = new HashSet<>(enemy.getMaxUnits());
      units.addAll(enemy.getMaxAmphibUnits());
      if (!units.isEmpty()) {
        armies.add(new Army(units, enemy.getMaxBombardUnits()));
      }
    }
    return armies;
  }

  /**
   * Runs the armies against {@code defenders} in order. Each army's capture chance comes from a
   * battle against the defenders that survive the previous army when it fails to capture. The chain
   * captures if any army does, taking the armies as independent: {@code 1 - prod(1 - capture_i)}.
   *
   * <p>The battle result only holds the survivors averaged over all runs, where a run the army won
   * counts as no survivors. Given a failed capture the average is too low by the share of runs won,
   * so {@link #survivorsGivenFailure} scales it by {@code 1 / (1 - capture)} and restores that many
   * of the fallen defenders, the most valuable first.
   */
  public static Chain run(
      final ProData proData,
      final ProOddsCalculator calc,
      final Territory t,
      final Collection<Unit> defenders,
      final List<Army> armies) {
    Collection<Unit> remaining = defenders;
    double noCapture = 1;
    final List<Double> armyCaptures = new ArrayList<>();
    for (final Army army : armies) {
      final ProBattleResult result =
          calc.estimateDefendBattleResults(
              proData, t, army.units(), remaining, army.bombardUnits());
      final double capture = Math.min(100, Math.max(0, result.getCapturePercentage()));
      armyCaptures.add(capture);
      noCapture *= 1 - capture / 100;
      // A battle that was not fought (eg air alone against land) reports no survivors
      if (result.getWinPercentage() > 0 || !result.getAverageDefendersRemaining().isEmpty()) {
        remaining =
            survivorsGivenFailure(
                proData, remaining, result.getAverageDefendersRemaining(), capture);
      }
    }
    final double capturePercentage = 100 * (1 - noCapture);
    double strongestAlone = 0;
    if (capturePercentage >= MIN_CAPTURE_PERCENTAGE && armies.size() > 1) {
      for (final Army army : armies) {
        strongestAlone =
            Math.max(
                strongestAlone,
                calc.estimateDefendBattleResults(
                        proData, t, army.units(), defenders, army.bombardUnits())
                    .getCapturePercentage());
      }
    }
    return new Chain(capturePercentage, armyCaptures, remaining, Math.min(100, strongestAlone));
  }

  /**
   * The defenders left after an army that fails to capture. {@code averageRemaining} is the average
   * over all runs of {@code starting}, including the runs the army captured, so it is scaled up by
   * {@code 1 / (1 - capturePercentage)} and capped at {@code starting}. The units that make up the
   * difference are the fallen ones worth the most.
   */
  static Collection<Unit> survivorsGivenFailure(
      final ProData proData,
      final Collection<Unit> starting,
      final Collection<Unit> averageRemaining,
      final double capturePercentage) {
    if (capturePercentage <= 0 || capturePercentage >= 100) {
      return averageRemaining;
    }
    final List<Unit> fallen = new ArrayList<>(starting);
    fallen.removeAll(averageRemaining);
    fallen.sort(Comparator.comparingInt((Unit u) -> proData.getUnitValue(u.getType())).reversed());
    final long scaled = Math.round(averageRemaining.size() / (1 - capturePercentage / 100));
    final int restored = (int) Math.min(fallen.size(), scaled - averageRemaining.size());
    final List<Unit> survivors = new ArrayList<>(averageRemaining);
    survivors.addAll(fallen.subList(0, Math.max(0, restored)));
    return survivors;
  }

  /**
   * What the owner of {@code t} loses if it falls, in PUs. A capital whose capture takes its
   * owner's treasury or income costs the treasury, {@link #RECAPTURE_HORIZON} rounds of income and
   * the factory. A factory that is no capital costs {@link #FACTORY_WEIGHT} of its territory's
   * income over the horizon and of the factory's TUV. 0 for any other territory.
   */
  public static double lossValue(final ProData proData, final Territory t) {
    final double treasury = ProUtils.capturedTreasury(t, proData.getData());
    final double income = ProUtils.capitalValue(t, proData.getData());
    final double territoryIncome =
        income > 0 ? 0 : (double) RECAPTURE_HORIZON * TerritoryAttachment.getProduction(t);
    final double factory =
        TuvUtils.getTuv(
            t.getMatches(Matches.unitCanProduceUnits().and(Matches.unitIsInfrastructure())),
            proData.getUnitValueMap());
    if (treasury > 0 || income > 0) {
      return treasury + RECAPTURE_HORIZON * income + territoryIncome + factory;
    }
    return factory > 0 ? FACTORY_WEIGHT * (territoryIncome + factory) : 0;
  }

  /**
   * Whether the chain's capture chance, weighed by what {@code t} is worth, calls for defence. Only
   * a combined attack counts: a territory one army takes by itself is what the callers already
   * judge, and defending it against the chain would hold back every other move for nothing.
   */
  public static boolean needsDefence(final ProData proData, final Territory t, final Chain chain) {
    return chain != null
        && chain.armyCapturePercentages().size() >= 2
        && chain.strongestAlonePercentage() < MIN_CAPTURE_PERCENTAGE
        && chain.capturePercentage() >= MIN_CAPTURE_PERCENTAGE
        && chain.capturePercentage() / 100 * lossValue(proData, t) >= MIN_EXPECTED_LOSS;
  }

  /**
   * Remembers chains by what they were run on, so asking again with the same defenders against the
   * same armies, as a defend loop does, doesn't simulate again. Clear it when a phase starts.
   */
  public static class Cache {
    private record Key(Territory t, Set<Unit> defenders, List<Army> armies) {}

    private final Map<Key, Chain> chains = new HashMap<>();

    public void clear() {
      chains.clear();
    }

    /**
     * Runs the armies that can attack {@code t} against {@code defenders} and logs the result.
     * Returns null when no army can attack.
     */
    public Chain runAndLog(
        final ProData proData,
        final ProOddsCalculator calc,
        final ProOtherMoveOptions enemyAttackOptions,
        final Territory t,
        final Collection<Unit> defenders) {
      final List<Army> armies = armies(enemyAttackOptions, t);
      if (armies.isEmpty()) {
        return null;
      }
      return chains.computeIfAbsent(
          new Key(t, new HashSet<>(defenders), armies),
          key -> {
            final Chain chain = run(proData, calc, t, defenders, armies);
            ProLogger.info(
                String.format(
                    "%s capture chain: %.1f%% from armies %s, lossValue=%.1f, alone=%.1f%%, "
                        + "defenders=%d, defendersRemaining=%d",
                    t.getName(),
                    chain.capturePercentage(),
                    chain.armyCapturePercentages(),
                    lossValue(proData, t),
                    chain.strongestAlonePercentage(),
                    defenders.size(),
                    chain.defendersRemaining().size()));
            return chain;
          });
    }
  }

  /** Runs the armies that can attack {@code t} against {@code defenders} and logs the result. */
  public static Chain runAndLog(
      final ProData proData,
      final ProOddsCalculator calc,
      final ProOtherMoveOptions enemyAttackOptions,
      final Territory t,
      final Collection<Unit> defenders) {
    return new Cache().runAndLog(proData, calc, enemyAttackOptions, t, defenders);
  }
}
