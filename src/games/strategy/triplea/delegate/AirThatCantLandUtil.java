package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;

/**
 * Utility for detecting and removing units that can't land at the end of a phase.
 */
public class AirThatCantLandUtil {
  private final IDelegateBridge m_bridge;

  public AirThatCantLandUtil(final IDelegateBridge bridge) {
    m_bridge = bridge;
  }

  public static boolean isLHTRCarrierProduction(final GameData data) {
    return games.strategy.triplea.Properties.getLHTRCarrierProductionRules(data);
  }

  public static boolean isLandExistingFightersOnNewCarriers(final GameData data) {
    return games.strategy.triplea.Properties.getLandExistingFightersOnNewCarriers(data);
  }

  public Collection<Territory> getTerritoriesWhereAirCantLand(final PlayerID player) {
    final GameData data = m_bridge.getData();
    final Collection<Territory> cantLand = new ArrayList<>();
    for (Territory current : data.getMap().getTerritories()) {
      final CompositeMatch<Unit> ownedAir = new CompositeMatchAnd<>();
      ownedAir.add(Matches.UnitIsAir);
      ownedAir.add(Matches.unitIsOwnedBy(player));
      final Collection<Unit> air = current.getUnits().getMatches(ownedAir);
      if (air.size() != 0 && !AirMovementValidator.canLand(air, current, player, data)) {
        cantLand.add(current);
      }
    }
    return cantLand;
  }

  public void removeAirThatCantLand(final PlayerID player, final boolean spareAirInSeaZonesBesideFactories) {
    final GameData data = m_bridge.getData();
    final GameMap map = data.getMap();
    for (Territory current : getTerritoriesWhereAirCantLand(player)) {
      final CompositeMatch<Unit> ownedAir = new CompositeMatchAnd<>();
      ownedAir.add(Matches.UnitIsAir);
      ownedAir.add(Matches.alliedUnit(player, data));
      final Collection<Unit> air = current.getUnits().getMatches(ownedAir);
      final boolean hasNeighboringFriendlyFactory =
          map.getNeighbors(current, Matches.territoryHasAlliedIsFactoryOrCanProduceUnits(data, player)).size() > 0;
      final boolean skip = spareAirInSeaZonesBesideFactories && current.isWater() && hasNeighboringFriendlyFactory;
      if (!skip) {
        removeAirThatCantLand(player, current, air);
      }
    }
  }

  private void removeAirThatCantLand(final PlayerID player, final Territory territory,
      final Collection<Unit> airUnits) {
    final Collection<Unit> toRemove = new ArrayList<>(airUnits.size());
    // if we cant land on land then none can
    if (!territory.isWater()) {
      toRemove.addAll(airUnits);
    } else
    // on water we may just no have enough carriers
    {
      // find the carrier capacity
      final Collection<Unit> carriers = territory.getUnits().getMatches(Matches.alliedUnit(player, m_bridge.getData()));
      int capacity = AirMovementValidator.carrierCapacity(carriers, territory);
      for (Unit unit : airUnits) {
        final UnitAttachment ua = UnitAttachment.get(unit.getType());
        final int cost = ua.getCarrierCost();
        if (cost == -1 || cost > capacity) {
          toRemove.add(unit);
        } else {
          capacity -= cost;
        }
      }
    }
    final Change remove = ChangeFactory.removeUnits(territory, toRemove);
    final String transcriptText = MyFormatter.unitsToTextNoOwner(toRemove) + " could not land in " + territory.getName()
        + " and " + (toRemove.size() > 1 ? "were" : "was") + " removed";
    m_bridge.getHistoryWriter().startEvent(transcriptText);
    m_bridge.addChange(remove);
  }
}
