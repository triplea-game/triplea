package games.strategy.triplea.ai.pro.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.AirBattle;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import games.strategy.triplea.delegate.power.calculator.PowerStrengthAndRolls;
import games.strategy.triplea.delegate.power.calculator.UnitPowerStrengthAndRolls;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.ToIntFunction;
import org.triplea.java.collections.CollectionUtils;

/**
 * Estimates the air battle that precedes a normal battle when "Battles May Be Preceeded By Air
 * Battles" is on: the attacking escorts fight the defending interceptors (using airAttack and
 * airDefense) before the main battle, and whoever dies there is missing from it.
 *
 * <p>This is an expected-value approximation, so the odds calculators can keep running one main
 * battle: many air battles are sampled and each side loses its average number of casualties,
 * cheapest units first. Not modeled: air battle retreats, suicide-on-attack units being removed
 * after an air battle, and the defender choosing not to launch interceptors.
 */
public final class ProAirBattleEstimator {
  private static final int SAMPLES = 200;
  private static final int MAX_ROUNDS_WHEN_UNLIMITED = 50;

  private ProAirBattleEstimator() {}

  /** The units expected to die in the air battle. */
  public record Outcome(List<Unit> attackersLost, List<Unit> defendersLost) {
    static final Outcome NONE = new Outcome(List.of(), List.of());

    public boolean isEmpty() {
      return attackersLost.isEmpty() && defendersLost.isEmpty();
    }
  }

  /**
   * Returns the expected casualties of the air battle before a normal battle, or an empty outcome
   * if the property is off or no escort faces an interceptor.
   *
   * @param unitValue cost of a unit, used to lose the cheapest units first
   */
  public static Outcome estimate(
      final GameData data,
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final ToIntFunction<Unit> unitValue,
      final Random random) {
    if (!Properties.getBattlesMayBePreceededByAirBattles(data.getProperties())) {
      return Outcome.NONE;
    }
    final List<Unit> escorts =
        CollectionUtils.getMatches(attackingUnits, Matches.unitCanAirBattle());
    if (escorts.isEmpty()) {
      return Outcome.NONE;
    }
    final List<Unit> interceptors =
        CollectionUtils.getMatches(
            defendingUnits,
            AirBattle.defendingGroundSeaBattleInterceptors(escorts.get(0).getOwner(), data));
    if (interceptors.isEmpty()) {
      return Outcome.NONE;
    }
    final Comparator<Unit> cheapestFirst = Comparator.comparingInt(unitValue);
    escorts.sort(cheapestFirst);
    interceptors.sort(cheapestFirst);

    final Side attackers = new Side(escorts, BattleState.Side.OFFENSE, data);
    final Side defenders = new Side(interceptors, BattleState.Side.DEFENSE, data);
    final int configuredRounds = Properties.getAirBattleRounds(data.getProperties());
    final int rounds = configuredRounds > 0 ? configuredRounds : MAX_ROUNDS_WHEN_UNLIMITED;

    long attackerDeaths = 0;
    long defenderDeaths = 0;
    for (int i = 0; i < SAMPLES; i++) {
      attackers.reset();
      defenders.reset();
      for (int round = 0;
          round < rounds && attackers.alive() > 0 && defenders.alive() > 0;
          round++) {
        if (!attackers.canHit() && !defenders.canHit()) {
          break;
        }
        final int hitsOnDefenders = attackers.rollHits(random);
        final int hitsOnAttackers = defenders.rollHits(random);
        defenders.takeHits(hitsOnDefenders);
        attackers.takeHits(hitsOnAttackers);
      }
      attackerDeaths += escorts.size() - attackers.alive();
      defenderDeaths += interceptors.size() - defenders.alive();
    }
    return new Outcome(
        escorts.subList(0, (int) Math.round((double) attackerDeaths / SAMPLES)),
        interceptors.subList(0, (int) Math.round((double) defenderDeaths / SAMPLES)));
  }

  /** One side of the air battle, with its units ordered the way casualties are taken. */
  private static final class Side {
    private final List<Double> hitChances = new ArrayList<>();
    private final List<Integer> rolls = new ArrayList<>();
    private final List<Boolean> bestRoll = new ArrayList<>();
    private final int[] maxHitPoints;
    private final int[] hitPoints;

    Side(final List<Unit> units, final BattleState.Side side, final GameData data) {
      final PowerStrengthAndRolls power =
          PowerStrengthAndRolls.build(
              units,
              CombatValueBuilder.airBattleCombatValue()
                  .side(side)
                  .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(data.getProperties()))
                  .gameDiceSides(data.getDiceSides())
                  .build());
      maxHitPoints = new int[units.size()];
      hitPoints = new int[units.size()];
      for (int i = 0; i < units.size(); i++) {
        final Unit unit = units.get(i);
        final UnitPowerStrengthAndRolls p = power.getTotalStrengthAndTotalRollsByUnit().get(unit);
        hitChances.add(Math.min(1.0, (double) p.getStrength() / p.getDiceSides()));
        rolls.add(p.getRolls());
        bestRoll.add(p.getChooseBestRoll());
        maxHitPoints[i] = unit.hitsUnitCanTakeHitWithoutBeingKilled() + 1;
      }
    }

    void reset() {
      System.arraycopy(maxHitPoints, 0, hitPoints, 0, hitPoints.length);
    }

    int alive() {
      int count = 0;
      for (final int hp : hitPoints) {
        if (hp > 0) {
          count++;
        }
      }
      return count;
    }

    boolean canHit() {
      for (int i = 0; i < hitPoints.length; i++) {
        if (hitPoints[i] > 0 && hitChances.get(i) > 0 && rolls.get(i) > 0) {
          return true;
        }
      }
      return false;
    }

    int rollHits(final Random random) {
      int hits = 0;
      for (int i = 0; i < hitPoints.length; i++) {
        if (hitPoints[i] <= 0) {
          continue;
        }
        final double chance = hitChances.get(i);
        if (bestRoll.get(i)) {
          if (rolls.get(i) > 0 && random.nextDouble() < 1 - Math.pow(1 - chance, rolls.get(i))) {
            hits++;
          }
        } else {
          for (int r = 0; r < rolls.get(i); r++) {
            if (random.nextDouble() < chance) {
              hits++;
            }
          }
        }
      }
      return hits;
    }

    /** Applies hits to the first (cheapest) living units. */
    void takeHits(final int hits) {
      int remaining = hits;
      for (int i = 0; i < hitPoints.length && remaining > 0; i++) {
        final int absorbed = Math.min(remaining, Math.max(0, hitPoints[i]));
        hitPoints[i] -= absorbed;
        remaining -= absorbed;
      }
    }
  }
}
