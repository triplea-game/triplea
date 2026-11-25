package games.strategy.triplea.ai.pro.data;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;

/** The result of an AI amphibious movement analysis. */
@Getter
public class ProTransport {
  private final Unit transport;
  private final Map<Territory, Set<Territory>> transportMap = new HashMap<>();
  private final Map<Territory, Set<Territory>> seaTransportMap = new HashMap<>();
  // Sea zones in seaTransportMap that hold enemy units: the transport may end there only behind a
  // sea battle the combat move wins.
  private final Set<Territory> blockedSeaTerritories = new HashSet<>();

  ProTransport(final Unit transport) {
    this.transport = transport;
  }

  void addTerritories(Set<Territory> attackTerritories, Set<Territory> loadFromTerritories) {
    for (Territory t : attackTerritories) {
      transportMap.computeIfAbsent(t, key -> new HashSet<>()).addAll(loadFromTerritories);
    }
  }

  void addSeaTerritories(Set<Territory> attackTerritories, Set<Territory> loadFromTerritories) {
    for (Territory t : attackTerritories) {
      seaTransportMap.computeIfAbsent(t, key -> new HashSet<>()).addAll(loadFromTerritories);
    }
  }

  void addBlockedSeaTerritory(
      final Territory seaTerritory,
      final Set<Territory> unloadTerritories,
      final Set<Territory> loadFromTerritories) {
    blockedSeaTerritories.add(seaTerritory);
    addTerritories(unloadTerritories, loadFromTerritories);
    addSeaTerritories(Set.of(seaTerritory), loadFromTerritories);
  }
}
