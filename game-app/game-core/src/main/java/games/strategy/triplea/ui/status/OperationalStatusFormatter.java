package games.strategy.triplea.ui.status;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.SupplyTerritoryAttachment;
import games.strategy.triplea.delegate.battle.AirControlTracker;
import games.strategy.triplea.delegate.supply.SupplyNetworkResolver;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;

/** Formats player-visible supply and air-control state for Swing map surfaces. */
public final class OperationalStatusFormatter {
  private OperationalStatusFormatter() {}

  public static @Nullable GamePlayer perspectivePlayer(
      final Territory territory, final List<GamePlayer> viewers, final GameState data) {
    final GamePlayer owner = territory.getOwner();
    for (final GamePlayer viewer : viewers) {
      if (viewer.equals(owner)
          || (data.getRelationshipTracker().getRelationship(viewer, owner) != null
              && data.getRelationshipTracker().isAllied(viewer, owner))) {
        return viewer;
      }
    }
    if (!viewers.isEmpty()) {
      return viewers.getFirst();
    }
    return owner;
  }

  public static int maxIsolationTurns(
      final Territory territory, final GamePlayer perspective, final GameState data) {
    return visibleFriendlyUnits(territory, perspective, data).stream()
        .mapToInt(unit -> SupplyNetworkResolver.getOutOfSupplyTurns(unit, data))
        .max()
        .orElse(0);
  }

  public static String territoryTooltip(
      final Territory territory, final GameState data, final List<GamePlayer> viewers) {
    final StringBuilder tooltip = new StringBuilder("<html><b>");
    tooltip.append(escape(territory.getName())).append("</b>");
    final GamePlayer perspective = perspectivePlayer(territory, viewers, data);

    if (SupplyNetworkResolver.isEnabled(data) && !territory.isWater() && perspective != null) {
      final boolean source =
          SupplyTerritoryAttachment.get(territory)
              .map(SupplyTerritoryAttachment::getSupplySource)
              .orElse(false);
      final boolean supplied = SupplyNetworkResolver.isSupplied(territory, perspective, data);
      final List<String> roads =
          SupplyNetworkResolver.getRoadNeighbors(territory, data).stream()
              .map(Territory::getName)
              .sorted()
              .toList();
      tooltip
          .append("<br>Supply source: ")
          .append(source ? "yes" : "no")
          .append("<br>Current road supply: ")
          .append(supplied ? "connected" : "cut")
          .append("<br>Road links: ")
          .append(roads.isEmpty() ? "none" : escape(String.join(", ", roads)));

      final List<Unit> isolated =
          visibleFriendlyUnits(territory, perspective, data).stream()
              .filter(unit -> SupplyNetworkResolver.getOutOfSupplyTurns(unit, data) > 0)
              .sorted(
                  Comparator.comparing((Unit unit) -> unit.getType().getName())
                      .thenComparing(unit -> unit.getId().toString()))
              .toList();
      if (!isolated.isEmpty()) {
        tooltip.append("<br><b>Isolated units</b>");
        final int removalTurns = SupplyNetworkResolver.getRemovalTurns(data);
        for (final Unit unit : isolated) {
          final int turns = SupplyNetworkResolver.getOutOfSupplyTurns(unit, data);
          tooltip
              .append("<br>")
              .append(escape(unit.getType().getName()))
              .append(": OOS ")
              .append(turns)
              .append("/")
              .append(removalTurns)
              .append(", movement blocked, removal in ")
              .append(Math.max(0, removalTurns - turns))
              .append(" owner turn(s)");
        }
      }
    }

    if (AirControlTracker.isEnabled(data)) {
      final AirControlTracker tracker = AirControlTracker.get(data);
      tooltip.append("<br>Air control: ");
      switch (tracker.getStatus(territory, data)) {
        case UNCONTROLLED -> tooltip.append("uncontrolled");
        case CONTESTED -> tooltip.append("contested");
        case CONTROLLED ->
            tooltip.append(
                tracker
                    .getController(territory, data)
                    .map(GamePlayer::getName)
                    .map(OperationalStatusFormatter::escape)
                    .orElse("unknown"));
      }
      if (tracker.getStatus(territory, data) != AirControlTracker.Status.UNCONTROLLED) {
        tooltip
            .append(" (")
            .append(AirControlTracker.isPersistent(data) ? "persistent" : "current round")
            .append(")");
      }
      if (perspective != null) {
        final int bonus = tracker.getGroundAttackBonus(territory, perspective, data);
        tooltip.append("<br>Friendly ground attack bonus: +").append(bonus);
      }
    }

    return tooltip.append("</html>").toString();
  }

  private static List<Unit> visibleFriendlyUnits(
      final Territory territory, final GamePlayer perspective, final GameState data) {
    return territory.getUnitCollection().getUnits().stream()
        .filter(SupplyNetworkResolver::requiresSupply)
        .filter(
            unit ->
                unit.isOwnedBy(perspective)
                    || data.getRelationshipTracker().isAllied(perspective, unit.getOwner()))
        .toList();
  }

  private static String escape(final String text) {
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}
