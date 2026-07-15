package games.strategy.triplea.delegate.supply;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.SupplyTerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nullable;

/** Resolves road-based supply reachability for a player. */
public final class SupplyNetworkResolver {
  public static final String SUPPLY_NETWORK_ENABLED = "Supply Network Enabled";
  public static final String OUT_OF_SUPPLY_REMOVAL_TURNS = "Out Of Supply Removal Turns";
  public static final int DEFAULT_REMOVAL_TURNS = 2;

  private static final Comparator<Territory> TERRITORY_ORDER =
      Comparator.comparing(Territory::getName);

  private SupplyNetworkResolver() {}

  public static boolean isEnabled(final GameState data) {
    return data.getProperties().get(SUPPLY_NETWORK_ENABLED, false);
  }

  public static int getRemovalTurns(final GameState data) {
    return Math.max(
        1, data.getProperties().get(OUT_OF_SUPPLY_REMOVAL_TURNS, DEFAULT_REMOVAL_TURNS));
  }

  public static Optional<SupplyTracker> getTracker(final GameState data) {
    if (!(data instanceof GameData gameData)) {
      return Optional.empty();
    }
    return gameData.getDelegates().stream()
        .filter(SupplyDelegate.class::isInstance)
        .map(SupplyDelegate.class::cast)
        .map(SupplyDelegate::getTracker)
        .findFirst();
  }

  public static int getOutOfSupplyTurns(final Unit unit, final GameState data) {
    return getTracker(data).map(tracker -> tracker.getOutOfSupplyTurns(unit)).orElse(0);
  }

  public static int getTurnsUntilRemoval(final Unit unit, final GameState data) {
    final int isolationTurns = getOutOfSupplyTurns(unit, data);
    return isolationTurns == 0 ? 0 : Math.max(0, getRemovalTurns(data) - isolationTurns);
  }

  public static boolean requiresSupply(final Unit unit) {
    return Matches.unitIsLand().test(unit);
  }

  public static boolean canMove(
      final Unit unit, final Territory start, final GamePlayer player, final GameState data) {
    return canMove(unit, start, player, data, getTracker(data).orElse(null));
  }

  static boolean canMove(
      final Unit unit,
      final Territory start,
      final GamePlayer player,
      final GameState data,
      final @Nullable SupplyTracker tracker) {
    if (!isEnabled(data) || start.isWater() || !requiresSupply(unit)) {
      return true;
    }
    return tracker == null
        ? isSupplied(start, player, data)
        : tracker.getOutOfSupplyTurns(unit) == 0;
  }

  public static boolean isSupplied(
      final Territory territory, final GamePlayer player, final GameState data) {
    return !isEnabled(data)
        || territory.isWater()
        || getSuppliedTerritories(player, data).contains(territory);
  }

  public static Set<Territory> getSuppliedTerritories(
      final GamePlayer player, final GameState data) {
    if (!isEnabled(data)) {
      return new LinkedHashSet<>(sortedTerritories(data.getMap().getTerritories()));
    }

    final Set<Territory> supplied = new LinkedHashSet<>();
    final ArrayDeque<Territory> pending = new ArrayDeque<>();
    for (final Territory source : getSupplySources(player, data)) {
      if (supplied.add(source)) {
        pending.addLast(source);
      }
    }

    while (!pending.isEmpty()) {
      final Territory current = pending.removeFirst();
      for (final Territory neighbor : getRoadNeighbors(current, data)) {
        if (isFriendlyLand(neighbor, player, data) && supplied.add(neighbor)) {
          pending.addLast(neighbor);
        }
      }
    }
    return supplied;
  }

  public static List<Territory> getSupplySources(final GamePlayer player, final GameState data) {
    return sortedTerritories(data.getMap().getTerritories()).stream()
        .filter(territory -> isFriendlyLand(territory, player, data))
        .filter(
            territory ->
                SupplyTerritoryAttachment.get(territory)
                    .map(SupplyTerritoryAttachment::getSupplySource)
                    .orElse(false))
        .toList();
  }

  public static List<Territory> getRoadNeighbors(final Territory territory, final GameState data) {
    final Set<Territory> neighbors = new TreeSet<>(TERRITORY_ORDER);
    SupplyTerritoryAttachment.get(territory)
        .ifPresent(attachment -> neighbors.addAll(attachment.getRoadConnections()));
    for (final Territory candidate : sortedTerritories(data.getMap().getTerritories())) {
      if (candidate.equals(territory)) {
        continue;
      }
      final boolean connectsBack =
          SupplyTerritoryAttachment.get(candidate)
              .map(SupplyTerritoryAttachment::getRoadConnections)
              .orElse(List.of())
              .contains(territory);
      if (connectsBack) {
        neighbors.add(candidate);
      }
    }
    return List.copyOf(neighbors);
  }

  public static List<Unit> getOutOfSupplyUnits(
      final Collection<Unit> units,
      final Territory territory,
      final GamePlayer player,
      final GameState data) {
    return units.stream()
        .filter(unit -> unit.isOwnedBy(player))
        .filter(SupplyNetworkResolver::requiresSupply)
        .filter(unit -> !canMove(unit, territory, player, data))
        .sorted(Comparator.comparing(unit -> unit.getId().toString()))
        .toList();
  }

  private static boolean isFriendlyLand(
      final Territory territory, final GamePlayer player, final GameState data) {
    if (territory.isWater()) {
      return false;
    }
    final GamePlayer owner = territory.getOwner();
    if (player.equals(owner)) {
      return true;
    }
    return data.getRelationshipTracker().getRelationship(player, owner) != null
        && data.getRelationshipTracker().isAllied(player, owner);
  }

  private static List<Territory> sortedTerritories(final Collection<Territory> territories) {
    final List<Territory> sorted = new ArrayList<>(territories);
    sorted.sort(TERRITORY_ORDER);
    return sorted;
  }
}
