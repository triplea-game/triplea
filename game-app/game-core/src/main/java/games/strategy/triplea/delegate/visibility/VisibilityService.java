package games.strategy.triplea.delegate.visibility;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Resolves the territories and units currently visible to one or more players. */
public final class VisibilityService {
  public static final String FOG_OF_WAR_ENABLED = "Fog Of War Enabled";
  public static final String FOG_OF_WAR_VISION_RADIUS = "Fog Of War Vision Radius";
  public static final int DEFAULT_VISION_RADIUS = 1;

  private static final Comparator<Territory> TERRITORY_ORDER =
      Comparator.comparing(Territory::getName);

  private VisibilityService() {}

  public static boolean isEnabled(final GameState data) {
    return data.getProperties().get(FOG_OF_WAR_ENABLED, false);
  }

  public static int getVisionRadius(final GameState data) {
    return Math.max(0, data.getProperties().get(FOG_OF_WAR_VISION_RADIUS, DEFAULT_VISION_RADIUS));
  }

  public static Set<Territory> getVisibleTerritories(
      final GamePlayer viewer, final GameState data) {
    return getVisibleTerritories(List.of(viewer), data);
  }

  public static Set<Territory> getVisibleTerritories(
      final Collection<GamePlayer> viewers, final GameState data) {
    final List<GamePlayer> activeViewers =
        viewers.stream().filter(viewer -> viewer != null && !viewer.isNull()).toList();
    if (!isEnabled(data) || activeViewers.isEmpty()) {
      return immutableSortedSet(data.getMap().getTerritories());
    }

    final Set<Territory> origins = new TreeSet<>(TERRITORY_ORDER);
    for (final Territory territory : data.getMap().getTerritories()) {
      if (isFriendlyToAny(territory.getOwner(), activeViewers, data)
          || territory.getUnitCollection().getUnits().stream()
              .map(Unit::getOwner)
              .anyMatch(owner -> isFriendlyToAny(owner, activeViewers, data))) {
        origins.add(territory);
      }
    }

    final Set<Territory> visible = new TreeSet<>(TERRITORY_ORDER);
    final int radius = getVisionRadius(data);
    for (final Territory origin : origins) {
      visible.add(origin);
      visible.addAll(data.getMap().getNeighbors(origin, radius));
    }
    return immutableSortedSet(visible);
  }

  public static boolean isVisible(
      final Territory territory, final GamePlayer viewer, final GameState data) {
    return isVisible(territory, List.of(viewer), data);
  }

  public static boolean isVisible(
      final Territory territory, final Collection<GamePlayer> viewers, final GameState data) {
    return getVisibleTerritories(viewers, data).contains(territory);
  }

  public static List<Unit> getVisibleUnits(
      final Territory territory, final Collection<GamePlayer> viewers, final GameState data) {
    if (!isVisible(territory, viewers, data)) {
      return List.of();
    }
    return territory.getUnitCollection().getUnits().stream()
        .sorted(Comparator.comparing(unit -> unit.getId().toString()))
        .toList();
  }

  private static boolean isFriendlyToAny(
      final GamePlayer candidate, final Collection<GamePlayer> viewers, final GameState data) {
    return viewers.stream().anyMatch(viewer -> isFriendly(candidate, viewer, data));
  }

  private static boolean isFriendly(
      final GamePlayer candidate, final GamePlayer viewer, final GameState data) {
    if (viewer.equals(candidate)) {
      return true;
    }
    return data.getRelationshipTracker().getRelationship(viewer, candidate) != null
        && data.getRelationshipTracker().isAllied(viewer, candidate);
  }

  private static Set<Territory> immutableSortedSet(final Collection<Territory> territories) {
    final List<Territory> sorted = new ArrayList<>(territories);
    sorted.sort(TERRITORY_ORDER);
    return Collections.unmodifiableSet(new LinkedHashSet<>(sorted));
  }
}
