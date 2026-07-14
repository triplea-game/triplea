package games.strategy.triplea.delegate.reinforcement;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.triplea.attachments.FixedReinforcementAttachment;
import games.strategy.triplea.delegate.StackCapacityResolver;
import java.util.ArrayList;
import java.util.List;

/** Places scheduled reinforcements and carries blocked units into a deterministic queue. */
public final class FixedReinforcementService {
  private FixedReinforcementService() {}

  public static void apply(
      final IDelegateBridge bridge,
      final GamePlayer player,
      final FixedReinforcementTracker tracker) {
    final GameData data = bridge.getData();
    final int currentRound = data.getSequence().getRound();
    if (!tracker.shouldProcess(player, currentRound)) {
      return;
    }
    final List<FixedReinforcementRule> schedule =
        FixedReinforcementAttachment.get(player)
            .map(FixedReinforcementAttachment::getReinforcements)
            .orElse(List.of());
    final List<FixedReinforcementOrder> orders =
        tracker.getOrdersForRound(player, currentRound, schedule);
    final List<FixedReinforcementOrder> remaining = new ArrayList<>();
    final IDelegateHistoryWriter history = bridge.getHistoryWriter();
    if (!orders.isEmpty()) {
      history.startEvent(
          "Fixed reinforcements for " + player.getName() + " in round " + currentRound);
    }
    for (final FixedReinforcementOrder order : orders) {
      final Territory territory = data.getMap().getTerritoryOrNull(order.territoryName());
      final UnitType unitType =
          data.getUnitTypeList().getUnitType(order.unitTypeName()).orElse(null);
      if (territory == null || unitType == null) {
        remaining.add(order);
        history.addChildToEvent(
            "Queued " + describe(order) + " because its map target is unavailable");
        continue;
      }
      if (!isFriendlyDestination(data, player, territory)) {
        remaining.add(order);
        history.addChildToEvent(
            "Queued " + describe(order) + " because " + territory.getName() + " is not allied");
        continue;
      }
      final List<Unit> candidates = unitType.createTemp(order.quantity(), player);
      final int acceptedCount =
          StackCapacityResolver.filterUnitsToFit(candidates, player, territory, List.of()).size();
      if (acceptedCount > 0) {
        final List<Unit> placed = unitType.create(acceptedCount, player);
        bridge.addChange(ChangeFactory.addUnits(territory, placed));
        history.addChildToEvent(
            "Placed " + acceptedCount + " " + unitType.getName() + " in " + territory.getName(),
            placed);
      }
      final int remainingQuantity = order.quantity() - acceptedCount;
      if (remainingQuantity > 0) {
        final FixedReinforcementOrder queued = order.withQuantity(remainingQuantity);
        remaining.add(queued);
        history.addChildToEvent("Queued " + describe(queued) + " because terrain capacity is full");
      }
    }
    tracker.completeRound(player, currentRound, remaining);
  }

  private static boolean isFriendlyDestination(
      final GameData data, final GamePlayer player, final Territory territory) {
    final GamePlayer owner = territory.getOwner();
    if (player.equals(owner)) {
      return true;
    }
    return data.getRelationshipTracker().getRelationship(player, owner) != null
        && data.getRelationshipTracker().isAllied(player, owner);
  }

  private static String describe(final FixedReinforcementOrder order) {
    return order.quantity()
        + " "
        + order.unitTypeName()
        + " for "
        + order.territoryName()
        + " (scheduled round "
        + order.scheduledRound()
        + ")";
  }
}
