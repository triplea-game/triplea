package games.strategy.triplea.ai.pro.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.attachments.AbstractConditionsAttachment;
import games.strategy.triplea.attachments.ICondition;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.ui.ObjectiveDummyDelegateBridge;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import lombok.experimental.UtilityClass;

/**
 * What national objectives make a territory worth, for the Hard Beta AI. A territory listed in an
 * objective's ownership test is worth the objective's PUs to my side when switching it between my
 * side and the enemy's completes or breaks that objective: mine completed or kept, theirs broken or
 * denied. The marginal territory counts in full (a set one short of completion, or a completed set
 * one capture breaks), each capture further away half as much, up to {@link #MAX_CAPTURES_TO_FLIP}.
 * Only objectives decided by a fixed list of territories count; other tests in the same objective
 * halve the value of completing it.
 */
@UtilityClass
public final class ProNationalObjectiveUtils {
  /** Objectives more captures than this from flipping add nothing. */
  static final int MAX_CAPTURES_TO_FLIP = 3;

  private static final Map<Object, Map<Territory, Double>> cache =
      new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(final Map.Entry<Object, Map<Territory, Double>> e) {
          return size() > 16;
        }
      };

  private record Rule(GamePlayer owner, RulesAttachment rule, RulesAttachment.OwnershipTest test) {}

  /** A game's objectives with an ownership test, and every territory they list. */
  private record Board(List<Rule> rules, List<Territory> listed) {}

  private static final Map<GameData, Board> boards = new WeakHashMap<>();

  /**
   * The weighted PUs of national objectives that ride on whether {@code t} is on {@code player}'s
   * side or the enemy's; 0 outside Hard Beta, with objectives off, or with nothing at stake.
   */
  public static double value(final ProData proData, final GamePlayer player, final Territory t) {
    if (!proData.isBeta()) {
      return 0;
    }
    return values(player).getOrDefault(t, 0.0);
  }

  /** Stake per territory for {@code player}'s side; cached per board. */
  static synchronized Map<Territory, Double> values(final GamePlayer player) {
    final GameData data = player.getData();
    final GameProperties props = data.getProperties();
    if (!Properties.getNationalObjectives(props)) {
      return Map.of();
    }
    final Board board = boards.computeIfAbsent(data, ProNationalObjectiveUtils::findBoard);
    if (board.rules().isEmpty()) {
      return Map.of();
    }
    final List<Object> key = new ArrayList<>(board.listed().size() + 3);
    key.add(System.identityHashCode(data));
    key.add(player);
    key.add(data.getSequence().getRound());
    board.listed().forEach(x -> key.add(x.getOwner()));
    return cache.computeIfAbsent(key, k -> compute(data, player, board.rules()));
  }

  private static Board findBoard(final GameData data) {
    final List<Rule> rules = findRules(data);
    final Set<Territory> listed = new HashSet<>();
    rules.forEach(r -> listed.addAll(r.test().territories()));
    return new Board(rules, List.copyOf(listed));
  }

  private static List<Rule> findRules(final GameData data) {
    final List<Rule> rules = new ArrayList<>();
    for (final GamePlayer p : data.getPlayerList().getPlayers()) {
      for (final RulesAttachment ra : RulesAttachment.getNationalObjectives(p)) {
        if (ra.getObjectiveValue() == 0) {
          continue;
        }
        ra.getOwnershipTest()
            .filter(test -> test.needed() > 0 && !test.territories().isEmpty())
            .ifPresent(test -> rules.add(new Rule(p, ra, test)));
      }
    }
    return rules;
  }

  private static Map<Territory, Double> compute(
      final GameData data, final GamePlayer player, final List<Rule> rules) {
    final Set<ICondition> conditions = new HashSet<>();
    for (final Rule r : rules) {
      if (r.rule().getUses() != 0) {
        conditions.add(r.rule());
      }
    }
    final Map<ICondition, Boolean> tested =
        AbstractConditionsAttachment.testAllConditionsRecursive(
            AbstractConditionsAttachment.getAllConditionsRecursive(conditions, null),
            null,
            new ObjectiveDummyDelegateBridge(data));
    final int puMultiplier = Properties.getPuMultiplier(data.getProperties());
    final Predicate<Territory> onMySide = Matches.isTerritoryAllied(player);
    final Map<Territory, Double> values = new HashMap<>();
    for (final Rule r : rules) {
      if (r.rule().getUses() == 0) {
        continue;
      }
      final RulesAttachment.OwnershipTest test = r.test();
      final double pus = (double) r.rule().getObjectiveValue() * puMultiplier;
      final boolean ruleOnMySide = player.equals(r.owner()) || player.isAllied(r.owner());
      final Predicate<GamePlayer> counts =
          test.direct()
              ? test.players()::contains
              : Matches.isAlliedWithAnyOfThesePlayers(test.players());
      int have = 0;
      for (final Territory x : test.territories()) {
        if (counts.test(x.getOwner())) {
          have++;
        }
      }
      final boolean satisfied = Boolean.TRUE.equals(tested.get(r.rule()));
      for (final Territory x : test.territories()) {
        final boolean countsNow = counts.test(x.getOwner());
        final boolean xOnMySide = onMySide.test(x);
        // After switching sides, x counts for the objective's owner iff it lands on that side.
        final boolean countsAfter = xOnMySide ? !ruleOnMySide : ruleOnMySide;
        if (countsAfter == countsNow) {
          continue;
        }
        // Captures still needed to flip the objective, this one included; the last one counts in
        // full, each earlier one half as much as the next: every territory in the set matters to
        // both sides, the one that completes or breaks it most.
        final int capturesToFlip;
        if (countsAfter) {
          capturesToFlip = satisfied ? 0 : test.needed() - have;
        } else {
          capturesToFlip = satisfied ? have - test.needed() + 1 : 0;
        }
        if (capturesToFlip < 1 || capturesToFlip > MAX_CAPTURES_TO_FLIP) {
          continue;
        }
        double change = Math.pow(0.5, capturesToFlip - 1.0);
        if (countsAfter && !test.ownershipOnly()) {
          // Ownership alone may not complete it: other tests (eg exclusions) may still fail.
          change *= 0.5;
        }
        final double satNow = satisfied ? 1 : 0;
        final double satAfter = satisfied ? 1 - change : change;
        // The objective owner's income with x on my side minus with x on the enemy's.
        final double ownerGainWithXMine = xOnMySide ? satNow - satAfter : satAfter - satNow;
        final double stake = (ruleOnMySide ? ownerGainWithXMine : -ownerGainWithXMine) * pus;
        if (stake != 0) {
          values.merge(x, stake, Double::sum);
        }
      }
    }
    values.values().removeIf(v -> v <= 0);
    return values;
  }
}
