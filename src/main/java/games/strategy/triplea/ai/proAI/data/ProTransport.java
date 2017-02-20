package games.strategy.triplea.ai.proAI.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;

public class ProTransport {

  private Unit transport;
  private Map<Territory, Set<Territory>> transportMap;
  private Map<Territory, Set<Territory>> seaTransportMap;

  public ProTransport(final Unit transport) {
    this.transport = transport;
    transportMap = new HashMap<>();
    seaTransportMap = new HashMap<>();
  }

  public void addTerritories(final Set<Territory> attackTerritories, final Set<Territory> myUnitsToLoadTerritories) {
    for (final Territory attackTerritory : attackTerritories) {
      if (transportMap.containsKey(attackTerritory)) {
        transportMap.get(attackTerritory).addAll(myUnitsToLoadTerritories);
      } else {
        final Set<Territory> territories = new HashSet<>();
        territories.addAll(myUnitsToLoadTerritories);
        transportMap.put(attackTerritory, territories);
      }
    }
  }

  public void addSeaTerritories(final Set<Territory> attackTerritories, final Set<Territory> myUnitsToLoadTerritories,
      final GameData data) {
    for (final Territory attackTerritory : attackTerritories) {
      if (seaTransportMap.containsKey(attackTerritory)) {
        seaTransportMap.get(attackTerritory).addAll(myUnitsToLoadTerritories);
      } else {
        final Set<Territory> territories = new HashSet<>();
        territories.addAll(myUnitsToLoadTerritories);
        seaTransportMap.put(attackTerritory, territories);
      }
    }
  }

  public void setTransport(final Unit transport) {
    this.transport = transport;
  }

  public Unit getTransport() {
    return transport;
  }

  public void setTransportMap(final Map<Territory, Set<Territory>> transportMap) {
    this.transportMap = transportMap;
  }

  public Map<Territory, Set<Territory>> getTransportMap() {
    return transportMap;
  }

  public void setSeaTransportMap(final Map<Territory, Set<Territory>> seaTransportMap) {
    this.seaTransportMap = seaTransportMap;
  }

  public Map<Territory, Set<Territory>> getSeaTransportMap() {
    return seaTransportMap;
  }

}
