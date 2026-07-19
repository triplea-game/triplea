package games.strategy.triplea.ai.smallfront;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.strategic.simulation.StrategicAction;
import games.strategy.triplea.delegate.strategic.simulation.StrategicActionResolver;
import games.strategy.triplea.delegate.supply.SupplyNetworkResolver;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Walks toward whichever objectives the enemy still holds, and takes one when the odds allow.
 *
 * <p>Deliberately simple. It exists because the Pro AI cannot play this map at all: with no
 * production frontier every unit is worth 0 TUV, so no attack ever scores as profitable and the AI
 * stands still for the whole game. Anything that moves toward the objectives is a better sparring
 * partner than that, and a better baseline to measure a trained policy against.
 */
public final class PressTheObjectivesPolicy implements SmallFrontPolicy {
  private static final int NO_PATH = 99;

  @Override
  public Optional<StrategicAction> choose(
      final List<StrategicAction> legalActions, final GameData data, final GamePlayer player) {
    final List<Territory> targets = enemyObjectives(data, player);
    return legalActions.stream()
        .filter(action -> !"end_phase".equals(action.type()))
        .map(action -> new Scored(action, score(action, data, player, targets)))
        .filter(scored -> scored.score > 0)
        .max(Comparator.comparingInt(Scored::score).thenComparing(scored -> key(scored.action())))
        .map(Scored::action);
  }

  private record Scored(StrategicAction action, int score) {}

  /** Ties are broken on the action's own text so a given position always plays the same way. */
  private static String key(final StrategicAction action) {
    return action.type() + action.parameters();
  }

  private static List<Territory> enemyObjectives(final GameData data, final GamePlayer player) {
    return data.getMap().getTerritories().stream()
        .filter(
            t -> TerritoryAttachment.get(t).map(TerritoryAttachment::getVictoryCity).orElse(0) > 0)
        .filter(t -> !player.equals(t.getOwner()))
        .sorted(Comparator.comparing(Territory::getName))
        .toList();
  }

  private int score(
      final StrategicAction action,
      final GameData data,
      final GamePlayer player,
      final List<Territory> targets) {
    final String routeText = action.parameters().get("route");
    final String unitIds = action.parameters().get("unitIds");
    if (routeText == null || unitIds == null) {
      return 0;
    }
    final Territory origin;
    final Territory destination;
    final int movingUnits;
    try {
      final var route = StrategicActionResolver.resolveRoute(data, routeText);
      origin = route.getStart();
      destination = route.getEnd();
      movingUnits = StrategicActionResolver.resolveUnits(unitIds, origin).size();
    } catch (final RuntimeException e) {
      // The generator offers probes into fog whose units may no longer resolve; skip them.
      return 0;
    }

    int score = 0;
    // Closing on an objective is the whole plan, so distance dominates.
    score += 10 * (distance(data, origin, targets) - distance(data, destination, targets));
    if (targets.contains(destination)) {
      score += 40;
    }
    if (!player.equals(destination.getOwner())) {
      score += 8;
      // Do not feed units into a defence that outnumbers them.
      final int defenders =
          (int)
              destination.getUnitCollection().getUnits().stream()
                  .filter(Matches.unitIsLand())
                  .filter(unit -> !unit.isOwnedBy(player))
                  .count();
      if (movingUnits <= defenders) {
        score -= 60;
      }
    }
    // wouldBeSupplied, not isSupplied: an attack target is never friendly land, so isSupplied says
    // no for every enemy territory and would penalise every attack this policy exists to make.
    if (!SupplyNetworkResolver.wouldBeSupplied(destination, player, data)) {
      score -= 25;
    }
    if (Boolean.parseBoolean(action.parameters().get("uncertain"))) {
      // A probe into fog may simply bounce, so prefer a known move of equal value.
      score -= 5;
    }
    return score;
  }

  private static int distance(
      final GameData data, final Territory from, final List<Territory> targets) {
    int best = NO_PATH;
    for (final Territory target : targets) {
      final int distance = data.getMap().getDistance(from, target, Matches.territoryIsLand());
      if (distance >= 0 && distance < best) {
        best = distance;
      }
    }
    return best;
  }
}
