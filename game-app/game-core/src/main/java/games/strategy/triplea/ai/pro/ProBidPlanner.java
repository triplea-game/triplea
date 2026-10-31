package games.strategy.triplea.ai.pro;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameSequence;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.triplea.ai.pro.data.ProBattleResult;
import games.strategy.triplea.ai.pro.data.ProPurchaseOption;
import games.strategy.triplea.ai.pro.data.ProTerritory;
import games.strategy.triplea.ai.pro.data.ProTerritoryManager;
import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.ai.pro.simulate.ProDummyDelegateBridge;
import games.strategy.triplea.ai.pro.util.ProOddsCalculator;
import games.strategy.triplea.ai.pro.util.ProUtils;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;

/**
 * Spends a bid on units that change round-1 battles, instead of the stock bid's spread of one unit
 * per territory by long-run territory value (which rarely changes a round-1 battle).
 *
 * <p>Simulations on copies of the board give this player's first combat move without a bid, and the
 * combat moves of enemies that move before it. Candidates are bundles of up to three land units for
 * one battle: an enemy territory this player can attack (placed where the units reach it in one
 * move, preferring spots the earlier enemies don't take), or an own territory those enemies attack
 * (placed there). The battle calculator ranks bundles by the change in {@code win% * value + TUV
 * swing} per PU (a target the combat move skipped counts only from the AI's minimum win
 * percentage). The best few are then checked by simulation: an attack bundle scores the mean gain
 * over three combat moves with it on the board (nothing when the move leaves it out), a defence
 * bundle the drop in the earlier enemies' simulated gains against this player. The best checked
 * bundle is taken and the search repeats; leftover PUs go to the stock bid logic.
 */
final class ProBidPlanner {

  /** One bid unit: where it goes, for which battle. */
  record Placement(Territory territory, UnitType unitType, Territory target, boolean attack) {}

  private static final int MAX_BUNDLE = 3;
  private static final double MIN_GAIN = 1.0;
  private static final double PRODUCTION_WEIGHT = 3;
  private static final double CAPITAL_VALUE = 20;
  private static final int ATTACK_TRIES = 3;
  private static final int DEFEND_TRIES = 2;
  private static final int ENEMY_RUNS = 2;
  private static final int ATTACK_RUNS = 3;

  private final AbstractProAi ai;
  private final ProData proData;
  private final ProOddsCalculator calc;
  private final ProCombatMoveAi combatMoveAi;
  private final Map<String, Double> scoreCache = new HashMap<>();

  ProBidPlanner(
      final AbstractProAi ai,
      final ProData proData,
      final ProOddsCalculator calc,
      final ProCombatMoveAi combatMoveAi) {
    this.ai = ai;
    this.proData = proData;
    this.calc = calc;
    this.combatMoveAi = combatMoveAi;
  }

  private record Target(
      Territory territory,
      boolean attack,
      boolean planned,
      List<Unit> attackers,
      List<Unit> defenders,
      Collection<Unit> bombard,
      Map<ProPurchaseOption, Territory> origins) {}

  private record Bundle(Target target, List<ProPurchaseOption> options, int cost, double gain) {}

  /**
   * A simulated combat move: attacking unit ids and {@code win% * value + swing} per attacked
   * territory, and the ids given to the bid units placed for it.
   */
  private record SimResult(
      Map<Territory, Set<UUID>> attackers,
      Map<Territory, Double> scores,
      Map<Placement, UUID> ids) {}

  /** Returns the bid units to place; empty when nothing beats the stock bid logic. */
  List<Placement> plan(final GameData data, final GamePlayer player, final int pus) {
    final GameStep combatStep = findFirstCombatMoveStep(data, player);
    if (combatStep == null) {
      return List.of();
    }
    final SimResult baseline = simulate(data, player, combatStep, List.of());
    proData.initialize(ai);
    if (baseline == null) {
      return List.of();
    }
    final Map<UUID, Unit> unitsById = new HashMap<>();
    for (final Territory t : data.getMap().getTerritories()) {
      t.getUnits().forEach(u -> unitsById.put(u.getId(), u));
    }

    // Attacks on this player planned by enemies that move before its first combat move
    final Map<Territory, List<Unit>> threats = new LinkedHashMap<>();
    final List<GameStep> enemySteps = findEnemyCombatMovesBefore(data, player, combatStep);
    double enemyNow = 0;
    for (int run = 0; run < ENEMY_RUNS; run++) {
      for (final GameStep step : enemySteps) {
        final SimResult enemy = simulate(data, player, step, List.of());
        proData.initialize(ai);
        if (enemy == null) {
          continue;
        }
        enemyNow += enemyScore(enemy, player) / ENEMY_RUNS;
        enemy
            .attackers()
            .forEach(
                (t, ids) -> {
                  if (!t.isWater() && t.isOwnedBy(player) && !threats.containsKey(t)) {
                    final List<Unit> units = new ArrayList<>();
                    ids.stream().map(unitsById::get).filter(Objects::nonNull).forEach(units::add);
                    threats.put(t, units);
                  }
                });
      }
    }

    final List<Territory> placeLand =
        landPlaceTerritories(data, player, proData.getMyUnitTerritories());
    final List<ProPurchaseOption> landOptions = new ArrayList<>();
    for (final ProPurchaseOption o : proData.getPurchaseOptions().getLandOptions()) {
      if (!o.isConstruction() && !o.isConsumesUnits() && o.getCost() > 0) {
        landOptions.add(o);
      }
    }
    if (placeLand.isEmpty() || landOptions.isEmpty()) {
      return List.of();
    }

    final ProTerritoryManager territoryManager = new ProTerritoryManager(calc, proData);
    territoryManager.populateAttackOptions();

    // Own territories an earlier-moving enemy attacks, and how likely the spot survives
    final Map<Territory, Double> survival = new HashMap<>();
    final List<Target> targets = new ArrayList<>();
    for (final Map.Entry<Territory, List<Unit>> e : threats.entrySet()) {
      final Territory t = e.getKey();
      if (!placeLand.contains(t) || e.getValue().isEmpty()) {
        continue;
      }
      final List<Unit> defenders = t.getMatches(Matches.isUnitAllied(player));
      final ProBattleResult r =
          calc.calculateBattleResults(proData, t, e.getValue(), defenders, List.of());
      survival.put(t, 1 - clampWin(r) / 100);
      final Map<ProPurchaseOption, Territory> origins = new LinkedHashMap<>();
      for (final ProPurchaseOption o : landOptions) {
        if (o.getDefense() > 0) {
          origins.put(o, t);
        }
      }
      targets.add(new Target(t, false, false, e.getValue(), defenders, List.of(), origins));
    }

    // Enemy land territories this player can attack in its first combat move
    final Set<UUID> used = new HashSet<>();
    baseline.attackers().values().forEach(used::addAll);
    final Predicate<Territory> through =
        Matches.territoryIsLand()
            .and(Matches.isTerritoryAllied(player))
            .and(Matches.territoryIsPassableAndNotRestricted(player));
    for (final Map.Entry<Territory, ProTerritory> e :
        territoryManager.getAttackOptions().getTerritoryMap().entrySet()) {
      final Territory target = e.getKey();
      if (target.isWater() || !Matches.isTerritoryEnemy(player).test(target)) {
        continue;
      }
      final boolean planned = baseline.attackers().containsKey(target);
      final List<Unit> attackers = new ArrayList<>();
      if (planned) {
        baseline.attackers().get(target).stream()
            .map(unitsById::get)
            .filter(Objects::nonNull)
            .forEach(attackers::add);
      } else {
        for (final Unit u : e.getValue().getMaxUnits()) {
          if (!used.contains(u.getId())) {
            attackers.add(u);
          }
        }
      }
      final Map<ProPurchaseOption, Territory> origins = new LinkedHashMap<>();
      for (final ProPurchaseOption o : landOptions) {
        if (o.getAttack() <= 0) {
          continue;
        }
        // Prefer the spot most likely to survive the earlier enemies, then the nearest
        Territory best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (final Territory t : placeLand) {
          final int d = data.getMap().getDistanceIgnoreEndForCondition(t, target, through);
          if (d < 1 || d > o.getMovement()) {
            continue;
          }
          final double s = survival.getOrDefault(t, 1.0);
          final double bestS = best == null ? 0 : survival.getOrDefault(best, 1.0);
          if (best == null || s > bestS || (s == bestS && d < bestDistance)) {
            best = t;
            bestDistance = d;
          }
        }
        if (best != null) {
          origins.put(o, best);
        }
      }
      if (!origins.isEmpty()) {
        targets.add(
            new Target(
                target,
                true,
                planned,
                attackers,
                e.getValue().getMaxEnemyDefenders(player),
                planned ? e.getValue().getBombardTerritoryMap().keySet() : List.of(),
                origins));
      }
    }

    // Greedy, one bundle at a time: rank bundles by calculator gain per PU, then check the best
    // few by simulation and take the best checked one.
    final Map<Target, List<ProPurchaseOption>> chosen = new LinkedHashMap<>();
    final List<Placement> placements = new ArrayList<>();
    SimResult current = baseline;
    int remaining = pus;
    while (true) {
      final List<Bundle> ranked = new ArrayList<>();
      for (final Target target : targets) {
        final List<ProPurchaseOption> already = chosen.getOrDefault(target, List.of());
        final double now = score(target, already);
        final List<ProPurchaseOption> allowed = new ArrayList<>(target.origins().keySet());
        allowed.sort(Comparator.comparing(o -> o.getUnitType().getName()));
        for (final List<ProPurchaseOption> bundle :
            bundles(allowed, ProPurchaseOption::getCost, remaining)) {
          final List<ProPurchaseOption> with = new ArrayList<>(already);
          with.addAll(bundle);
          final double gain = (score(target, with) - now) * survival(target, bundle, survival);
          final int cost = bundle.stream().mapToInt(ProPurchaseOption::getCost).sum();
          if (gain >= MIN_GAIN) {
            ranked.add(new Bundle(target, bundle, cost, gain));
          }
        }
      }
      ranked.sort(Comparator.comparingDouble((Bundle x) -> -x.gain() / x.cost()));
      Bundle best = null;
      SimResult bestSim = null;
      double bestEnemy = enemyNow;
      int attackTries = 0;
      int defendTries = 0;
      for (final Bundle candidate : ranked) {
        if (best != null && candidate.gain() / candidate.cost() <= best.gain() / best.cost()) {
          break;
        }
        if (candidate.target().attack()) {
          if (attackTries++ >= ATTACK_TRIES) {
            continue;
          }
        } else if (defendTries++ >= DEFEND_TRIES) {
          continue;
        }
        final List<Placement> trial = new ArrayList<>(placements);
        final List<Placement> added = toPlacements(candidate.target(), candidate.options());
        trial.addAll(added);
        if (!candidate.target().attack()) {
          // Replay the earlier enemies' combat moves with the bundle in place
          double enemyWith = 0;
          for (int run = 0; run < ENEMY_RUNS; run++) {
            for (final GameStep step : enemySteps) {
              final SimResult enemy = simulate(data, player, step, trial);
              proData.initialize(ai);
              enemyWith += enemy == null ? 0 : enemyScore(enemy, player) / ENEMY_RUNS;
            }
          }
          final double gain = enemyNow - enemyWith;
          if (gain >= MIN_GAIN
              && (best == null || gain / candidate.cost() > best.gain() / best.cost())) {
            best = new Bundle(candidate.target(), candidate.options(), candidate.cost(), gain);
            bestSim = null;
            bestEnemy = enemyWith;
          }
          continue;
        }
        // The combat move is not deterministic (battle calculator): the gain is the mean over a
        // few simulations, counting nothing when the combat move leaves the bundle out
        final Territory t = candidate.target().territory();
        SimResult sim = null;
        double gainSum = 0;
        for (int run = 0; run < ATTACK_RUNS; run++) {
          final SimResult one = simulate(data, player, combatStep, trial);
          proData.initialize(ai);
          if (one == null) {
            continue;
          }
          final Set<UUID> attackers = one.attackers().getOrDefault(t, Set.of());
          if (added.stream().anyMatch(p -> attackers.contains(one.ids().get(p)))) {
            gainSum += one.scores().getOrDefault(t, 0.0) - current.scores().getOrDefault(t, 0.0);
            sim = one;
          }
        }
        final double gain =
            gainSum / ATTACK_RUNS * survival(candidate.target(), candidate.options(), survival);
        if (sim != null
            && gain >= MIN_GAIN
            && (best == null || gain / candidate.cost() > best.gain() / best.cost())) {
          best = new Bundle(candidate.target(), candidate.options(), candidate.cost(), gain);
          bestSim = sim;
        }
      }
      if (best == null) {
        break;
      }
      ProLogger.info(
          String.format(
              "Bid: %s %s with %s, gain=%.1f cost=%d",
              best.target().attack() ? "attack" : "defend",
              best.target().territory().getName(),
              best.options().stream().map(o -> o.getUnitType().getName()).toList(),
              best.gain(),
              best.cost()));
      chosen.computeIfAbsent(best.target(), k -> new ArrayList<>()).addAll(best.options());
      placements.addAll(toPlacements(best.target(), best.options()));
      if (bestSim != null) {
        current = bestSim;
      } else {
        enemyNow = bestEnemy;
      }
      remaining -= best.cost();
    }
    return placements;
  }

  /** For an attack, the lowest survival chance among the bundle's placement spots. */
  private static double survival(
      final Target target,
      final List<ProPurchaseOption> bundle,
      final Map<Territory, Double> survival) {
    double s = 1;
    if (target.attack()) {
      for (final ProPurchaseOption o : bundle) {
        s = Math.min(s, survival.getOrDefault(target.origins().get(o), 1.0));
      }
    }
    return s;
  }

  private static List<Placement> toPlacements(
      final Target target, final List<ProPurchaseOption> options) {
    final List<Placement> out = new ArrayList<>();
    for (final ProPurchaseOption o : options) {
      for (int i = 0; i < o.getQuantity(); i++) {
        out.add(
            new Placement(
                target.origins().get(o), o.getUnitType(), target.territory(), target.attack()));
      }
    }
    return out;
  }

  /**
   * The target's {@code win% * value + swing} with {@code extra} units added to the attackers (or,
   * for a threatened own territory, minus the enemy's score with them added to the defenders).
   */
  private double score(final Target target, final List<ProPurchaseOption> extra) {
    final List<String> names = new ArrayList<>();
    extra.forEach(o -> names.add(o.getUnitType().getName()));
    names.sort(Comparator.naturalOrder());
    final String key = target.territory().getName() + "|" + target.attack() + "|" + names;
    final Double cached = scoreCache.get(key);
    if (cached != null) {
      return cached;
    }
    final List<Unit> added = new ArrayList<>();
    for (final ProPurchaseOption o : extra) {
      added.addAll(o.createTempUnits());
    }
    final double value = value(target.territory());
    final double score;
    if (target.attack()) {
      final List<Unit> attackers = new ArrayList<>(target.attackers());
      attackers.addAll(added);
      if (attackers.stream().noneMatch(Matches.unitIsLand())) {
        score = 0;
      } else {
        final ProBattleResult r =
            calc.calculateBattleResults(
                proData, target.territory(), attackers, target.defenders(), target.bombard());
        final double win = clampWin(r);
        score =
            !target.planned() && win < proData.getMinWinPercentage()
                ? 0
                : win / 100 * value + clampSwing(r.getTuvSwing());
      }
    } else {
      final List<Unit> defenders = new ArrayList<>(target.defenders());
      defenders.addAll(added);
      final ProBattleResult r =
          calc.calculateBattleResults(
              proData, target.territory(), target.attackers(), defenders, target.bombard());
      score = -(clampWin(r) / 100 * value + clampSwing(r.getTuvSwing()));
    }
    scoreCache.put(key, score);
    return score;
  }

  /** Sum of {@code win% * value + swing} over an enemy's simulated attacks on the bidder's land. */
  private static double enemyScore(final SimResult enemy, final GamePlayer bidder) {
    double sum = 0;
    for (final Map.Entry<Territory, Double> e : enemy.scores().entrySet()) {
      if (!e.getKey().isWater() && e.getKey().isOwnedBy(bidder)) {
        sum += e.getValue();
      }
    }
    return sum;
  }

  static double value(final Territory t) {
    final ProCombatMoveAi.ProductionAndIsCapital pc = ProCombatMoveAi.getProductionAndIsCapital(t);
    final int isCapital = ProUtils.captureStopsIncome(t, t.getData().getMap()) ? 1 : 0;
    return PRODUCTION_WEIGHT * pc.production + CAPITAL_VALUE * isCapital;
  }

  private static double clampWin(final ProBattleResult r) {
    return Math.max(0, Math.min(100, r.getWinPercentage()));
  }

  /** The calculator's early exits report +-999 swings; keep them from dominating a sum. */
  static double clampSwing(final double swing) {
    return Math.max(-200, Math.min(200, swing));
  }

  /**
   * Multisets of 1 to {@link #MAX_BUNDLE} of {@code options} whose total cost fits {@code budget},
   * each in {@code options} order.
   */
  static <T> List<List<T>> bundles(
      final List<T> options, final ToIntFunction<T> cost, final int budget) {
    final List<List<T>> out = new ArrayList<>();
    addBundles(options, cost, 0, new ArrayList<>(), 0, budget, out);
    return out;
  }

  private static <T> void addBundles(
      final List<T> options,
      final ToIntFunction<T> cost,
      final int from,
      final List<T> current,
      final int currentCost,
      final int budget,
      final List<List<T>> out) {
    if (!current.isEmpty()) {
      out.add(List.copyOf(current));
    }
    if (current.size() >= MAX_BUNDLE) {
      return;
    }
    for (int i = from; i < options.size(); i++) {
      final T o = options.get(i);
      final int c = currentCost + cost.applyAsInt(o);
      if (c > budget) {
        continue;
      }
      current.add(o);
      addBundles(options, cost, i, current, c, budget, out);
      current.remove(current.size() - 1);
    }
  }

  /**
   * Plays {@code combatStep}'s combat move on a copy of the board with the given bid units added.
   * Returns the attacked territories (of {@code data}) and the ids of the units attacking each.
   */
  private @Nullable SimResult simulate(
      final GameData data,
      final GamePlayer bidder,
      final GameStep combatStep,
      final List<Placement> placements) {
    final GameData copy = ai.copyData(data);
    if (copy == null) {
      return null;
    }
    final GamePlayer playerCopy =
        copy.getPlayerList().getPlayerId(combatStep.getPlayerId().getName());
    final GamePlayer bidderCopy = copy.getPlayerList().getPlayerId(bidder.getName());
    final Map<Placement, UUID> ids = new HashMap<>();
    for (final Placement p : placements) {
      final Territory t = copy.getMap().getTerritoryOrThrow(p.territory().getName());
      final List<Unit> units =
          copy.getUnitTypeList().getUnitTypeOrThrow(p.unitType().getName()).create(1, bidderCopy);
      copy.performChange(ChangeFactory.addUnits(t, units));
      ids.put(p, units.get(0).getId());
    }
    copy.getSequence()
        .setRoundAndStep(data.getSequence().getRound(), combatStep.getDisplayName(), playerCopy);
    final IMoveDelegate moveDel = copy.getMoveDelegate();
    moveDel.setDelegateBridgeAndPlayer(new ProDummyDelegateBridge(ai, playerCopy, copy));
    proData.initializeSimulation(ai, copy, playerCopy);
    final Map<Territory, ProTerritory> moveMap = combatMoveAi.doCombatMove(moveDel);
    final Map<Territory, Set<UUID>> attackers = new LinkedHashMap<>();
    final Map<Territory, Double> scores = new LinkedHashMap<>();
    for (final Map.Entry<Territory, ProTerritory> e : moveMap.entrySet()) {
      final Set<UUID> attackerIds = new HashSet<>();
      e.getValue().getUnits().forEach(u -> attackerIds.add(u.getId()));
      e.getValue()
          .getAmphibAttackMap()
          .values()
          .forEach(l -> l.forEach(u -> attackerIds.add(u.getId())));
      if (attackerIds.isEmpty()) {
        continue;
      }
      final Territory t = data.getMap().getTerritoryOrThrow(e.getKey().getName());
      attackers.put(t, attackerIds);
      final ProBattleResult r = e.getValue().getBattleResult();
      if (r != null) {
        scores.put(t, clampWin(r) / 100 * value(t) + clampSwing(r.getTuvSwing()));
      }
    }
    return new SimResult(attackers, scores, ids);
  }

  private static @Nullable GameStep findFirstCombatMoveStep(
      final GameData data, final GamePlayer player) {
    final GameSequence sequence = data.getSequence();
    for (int i = sequence.getStepIndex() + 1; i < sequence.size(); i++) {
      final GameStep step = sequence.getStep(i);
      if (player.equals(step.getPlayerId()) && isCombatMove(step)) {
        return step;
      }
    }
    return null;
  }

  /** Enemy combat moves between now and this player's first combat move, in turn order. */
  private static List<GameStep> findEnemyCombatMovesBefore(
      final GameData data, final GamePlayer player, final GameStep combatStep) {
    final List<GameStep> result = new ArrayList<>();
    final GameSequence sequence = data.getSequence();
    for (int i = sequence.getStepIndex() + 1; i < sequence.size(); i++) {
      final GameStep step = sequence.getStep(i);
      if (step == combatStep) {
        break;
      }
      final GamePlayer p = step.getPlayerId();
      if (p != null && isCombatMove(step) && player.isAtWar(p)) {
        result.add(step);
      }
    }
    return result;
  }

  private static boolean isCombatMove(final GameStep step) {
    return GameStep.isCombatMoveStepName(step.getName())
        && !GameStep.isAirborneCombatMoveStepName(step.getName());
  }

  /** Land territories a bid unit can be placed in. */
  private static List<Territory> landPlaceTerritories(
      final GameData data, final GamePlayer player, final Collection<Territory> unitTerritories) {
    final Set<Territory> candidates = new HashSet<>(data.getMap().getTerritoriesOwnedBy(player));
    candidates.addAll(unitTerritories);
    final Predicate<Territory> placeable =
        Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(
            player, false, false, false, false, false);
    final List<Territory> out = new ArrayList<>();
    for (final Territory t : candidates) {
      if (!t.isWater() && placeable.test(t)) {
        out.add(t);
      }
    }
    out.sort(Comparator.comparing(Territory::getName));
    return out;
  }
}
