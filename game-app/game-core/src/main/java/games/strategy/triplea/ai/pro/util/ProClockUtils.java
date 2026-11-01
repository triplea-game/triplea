package games.strategy.triplea.ai.pro.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ai.pro.data.ProPurchaseOption;
import games.strategy.triplea.ai.pro.data.ProPurchaseOptionMap;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import lombok.experimental.UtilityClass;

/**
 * Kill-strike clock: rounds until {@code attackers} can have at least {@link #RATIO} times the most
 * defence a capital's side can put there, counting their buys (bought as one unit mix) and their
 * marches. The defence is an upper bound: every unit of the capital's side that can reach it, plus
 * the owner's best defensive buy at the capital every turn. Strength is {@link
 * ProBattleUtils#estimateStrength}'s 2 x hit points + attack or defence, without support.
 */
@UtilityClass
public class ProClockUtils {
  public static final int NONE = 99;
  private static final double RATIO = 1.2;

  /** How the attackers spend their PUs while the clock runs. */
  private enum Mix {
    /** Best strength per PU, whatever its speed (eg infantry and artillery). */
    ECONOMY,
    /** Best strength per PU among the fastest land units (eg armour). */
    FAST
  }

  /**
   * Returns the shorter clock of the two unit mixes in rounds, or {@link #NONE} if it exceeds
   * {@code maxRounds}. Round t is the attackers' t-th turn from now.
   *
   * @param attackersBuyNow true in an attacker's own purchase phase: its PUs now buy units that can
   *     move next turn
   */
  public static int timeToKill(
      final GameData data,
      final Collection<GamePlayer> attackers,
      final Territory capital,
      final boolean attackersBuyNow,
      final int maxRounds) {
    int best = NONE;
    for (final Mix mix : Mix.values()) {
      final Curves c = curves(data, attackers, capital, mix, attackersBuyNow, maxRounds);
      double a = 0;
      double d = c.defence[0];
      for (int t = 1; t <= maxRounds && t < best; t++) {
        a += c.attack[t];
        d += c.defence[t];
        if (a + c.attackPerPu[t] >= RATIO * (d + t * c.defencePerTurn)) {
          best = t;
        }
      }
    }
    return best;
  }

  /**
   * Defensive strength the capital's owner must add this turn so that no round up to {@code rounds}
   * is a kill, if it buys its best defence at the capital on every later turn. 0 if none is needed.
   * Strength is 2 x hit points + defence, as the clock counts it.
   */
  public static double defenceDeficit(
      final GameData data,
      final Collection<GamePlayer> attackers,
      final Territory capital,
      final int rounds) {
    double deficit = 0;
    for (final Mix mix : Mix.values()) {
      final Curves c = curves(data, attackers, capital, mix, false, rounds);
      double a = 0;
      double d = c.defence[0];
      for (int t = 1; t <= rounds; t++) {
        a += c.attack[t];
        d += c.defence[t];
        deficit =
            Math.max(deficit, (a + c.attackPerPu[t]) / RATIO - (d + (t - 1) * c.defencePerTurn));
      }
    }
    return deficit;
  }

  /** Per-round strength the attackers add and the defence can add, as the clock counts them. */
  private record Curves(
      double[] attack, double[] attackPerPu, double[] defence, double defencePerTurn) {}

  private static Curves curves(
      final GameData data,
      final Collection<GamePlayer> attackers,
      final Territory capital,
      final Mix mix,
      final boolean attackersBuyNow,
      final int maxRounds) {
    final GamePlayer owner = capital.getOwner();
    final GameMap map = data.getMap();
    final Map<Territory, Integer> land =
        bfs(
            map,
            capital,
            t ->
                !t.isWater()
                    && Matches.territoryIsImpassable().negate().test(t)
                    && !ProUtils.isNeutralLand(t));
    final Map<Territory, Integer> any = bfs(map, capital, Matches.territoryIsImpassable().negate());
    final int horizon = maxRounds + 1;
    final double[] attack = new double[horizon + 1];
    final double[] defence = new double[horizon + 1];
    final Map<Unit, Integer> landArrival = new HashMap<>();

    for (final Territory t : map.getTerritories()) {
      for (final Unit u : t.getUnits()) {
        if (Matches.unitIsInfrastructure().test(u) || Matches.unitIsSea().test(u)) {
          continue;
        }
        final GamePlayer o = u.getOwner();
        final UnitAttachment ua = u.getUnitAttachment();
        final int move = Math.max(1, ua.getMovement(o));
        final boolean air = Matches.unitIsAir().test(u);
        if (attackers.contains(o)) {
          if (t.equals(capital)) {
            continue;
          }
          final int arrival;
          if (air) {
            final Integer d = any.get(t);
            arrival = d == null ? NONE : airAttackArrival(d, move);
          } else {
            final Integer d = t.isWater() ? null : land.get(t);
            arrival = d == null ? NONE : Math.max(1, ceilDiv(d, move));
          }
          landArrival.put(u, arrival);
          if (air || arrival < NONE) {
            add(attack, arrival, 2.0 * ua.getHitPoints() + ua.getAttack(o));
          }
        } else if (data.getRelationshipTracker().isAllied(o, owner)) {
          final Integer d = air ? any.get(t) : (t.isWater() ? null : land.get(t));
          if (d != null) {
            add(defence, d == 0 ? 0 : ceilDiv(d, move), 2.0 * ua.getHitPoints() + ua.getDefense(o));
          }
        }
      }
    }
    addAmphibious(data, attackers, land, landArrival, attack);

    final Resource pus = data.getResourceList().getResourceOrThrow(Constants.PUS);
    final double[] attackPerPu = new double[horizon + 1];
    for (final GamePlayer a : attackers) {
      final double[] best = bestAttackPerPu(a, data, mix);
      final int speed = (int) best[1];
      int factoryDistance = NONE;
      for (final Territory f : map.getTerritories()) {
        if (f.isOwnedBy(a)
            && ProMatches.territoryHasInfraFactoryAndIsLand().test(f)
            && land.containsKey(f)) {
          factoryDistance = Math.min(factoryDistance, land.get(f));
        }
      }
      if (factoryDistance == NONE || speed <= 0) {
        continue;
      }
      final double income = ProUtils.getPlayerProduction(a, data);
      final int bank = a.getResources().getQuantity(pus);
      for (int t = 1; t <= horizon; t++) {
        for (int i = attackersBuyNow ? 0 : 1; i < t; i++) {
          if (factoryDistance <= (t - i) * speed) {
            attackPerPu[t] += (i == 0 ? bank : income) * best[0];
          }
        }
      }
    }
    final double defencePerTurn = bestDefenceBuy(owner, data, capital);

    return new Curves(attack, attackPerPu, defence, defencePerTurn);
  }

  /**
   * Land units next to (or already on) the attackers' transports may arrive sooner by sea: one
   * trip, sailing 2 zones a turn, landing anywhere then marching 1 a turn. Best units first, up to
   * each zone's transport capacity.
   */
  private static void addAmphibious(
      final GameData data,
      final Collection<GamePlayer> attackers,
      final Map<Territory, Integer> land,
      final Map<Unit, Integer> landArrival,
      final double[] attack) {
    final GameMap map = data.getMap();
    for (final Territory z : map.getTerritories()) {
      if (!z.isWater()) {
        continue;
      }
      for (final GamePlayer a : attackers) {
        final List<Unit> transports =
            z.getMatches(Matches.unitIsOwnedBy(a).and(Matches.unitIsSeaTransport()));
        if (transports.isEmpty()) {
          continue;
        }
        int capacity = 0;
        for (final Unit tr : transports) {
          capacity += Math.max(0, tr.getUnitAttachment().getTransportCapacity());
        }
        final Map<Territory, Integer> sea = bfs(map, z, Territory::isWater);
        int arrival = NONE;
        for (final Map.Entry<Territory, Integer> e : land.entrySet()) {
          int s = NONE;
          for (final Territory n : map.getNeighbors(e.getKey(), Territory::isWater)) {
            s = Math.min(s, sea.getOrDefault(n, NONE));
          }
          if (s < NONE) {
            arrival = Math.min(arrival, Math.max(1, ceilDiv(s, 2)) + e.getValue());
          }
        }
        if (arrival == NONE) {
          continue;
        }
        final List<Unit> pool = new ArrayList<>(z.getMatches(landUnitOf(a)));
        for (final Territory n : map.getNeighbors(z, Matches.territoryIsLand())) {
          if (n.isOwnedBy(a)) {
            pool.addAll(n.getMatches(landUnitOf(a)));
          }
        }
        pool.sort(Comparator.comparingDouble((Unit u) -> -attackStrength(u)));
        for (final Unit u : pool) {
          final int cost = Math.max(1, u.getUnitAttachment().getTransportCost());
          final int before = landArrival.getOrDefault(u, NONE);
          if (cost > capacity || arrival >= before) {
            continue;
          }
          capacity -= cost;
          if (before < NONE) {
            add(attack, before, -attackStrength(u));
          }
          add(attack, arrival, attackStrength(u));
          landArrival.put(u, arrival);
        }
      }
    }
  }

  private static Predicate<Unit> landUnitOf(final GamePlayer a) {
    return Matches.unitIsOwnedBy(a)
        .and(Matches.unitIsLand())
        .and(Matches.unitIsInfrastructure().negate());
  }

  private static double attackStrength(final Unit u) {
    return 2.0 * u.getUnitAttachment().getHitPoints()
        + u.getUnitAttachment().getAttack(u.getOwner());
  }

  /** {strength per PU, movement} of the mix's unit. */
  private static double[] bestAttackPerPu(
      final GamePlayer player, final GameData data, final Mix mix) {
    final List<ProPurchaseOption> options =
        new ProPurchaseOptionMap(player, data)
            .getLandOptions().stream().filter(o -> o.getCost() > 0 && o.getAttack() > 0).toList();
    final int fastest = options.stream().mapToInt(ProPurchaseOption::getMovement).max().orElse(0);
    double best = 0;
    int speed = 0;
    for (final ProPurchaseOption o : options) {
      if (mix == Mix.FAST && o.getMovement() < fastest) {
        continue;
      }
      final double perPu = (2.0 * o.getHitPoints() + o.getAttack()) / o.getCost();
      if (perPu > best) {
        best = perPu;
        speed = o.getMovement();
      }
    }
    return new double[] {best, speed};
  }

  /** Defensive strength the owner can buy and place at the capital in one turn. */
  private static double bestDefenceBuy(
      final GamePlayer owner, final GameData data, final Territory capital) {
    if (!ProMatches.territoryHasInfraFactoryAndIsLand().test(capital)) {
      return 0;
    }
    ProPurchaseOption best = null;
    for (final ProPurchaseOption o : new ProPurchaseOptionMap(owner, data).getLandOptions()) {
      if (o.getCost() > 0
          && (best == null
              || (2.0 * o.getHitPoints() + o.getDefense()) / o.getCost()
                  > (2.0 * best.getHitPoints() + best.getDefense()) / best.getCost())) {
        best = o;
      }
    }
    if (best == null) {
      return 0;
    }
    final double income = ProUtils.getPlayerProduction(owner, data);
    final double maxPus =
        (double) TerritoryAttachment.getProduction(capital) / best.getQuantity() * best.getCost();
    return Math.min(income, maxPus)
        * (2.0 * best.getHitPoints() + best.getDefense())
        / best.getCost();
  }

  private static int airAttackArrival(final int d, final int move) {
    final int half = move / 2;
    return d <= half ? 1 : 1 + ceilDiv(d - half, move);
  }

  private static void add(final double[] byRound, final int round, final double value) {
    if (round < byRound.length) {
      byRound[round] += value;
    }
  }

  private static int ceilDiv(final int a, final int b) {
    return (a + b - 1) / b;
  }

  private static Map<Territory, Integer> bfs(
      final GameMap map, final Territory from, final Predicate<Territory> passable) {
    final Map<Territory, Integer> d = new HashMap<>();
    d.put(from, 0);
    final ArrayDeque<Territory> queue = new ArrayDeque<>(Set.of(from));
    while (!queue.isEmpty()) {
      final Territory x = queue.poll();
      for (final Territory y : map.getNeighbors(x)) {
        if (!d.containsKey(y) && passable.test(y)) {
          d.put(y, d.get(x) + 1);
          queue.add(y);
        }
      }
    }
    return d;
  }
}
