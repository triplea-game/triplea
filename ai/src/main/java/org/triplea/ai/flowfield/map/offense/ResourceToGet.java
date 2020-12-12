package org.triplea.ai.flowfield.map.offense;

import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.triplea.ai.flowfield.map.DiffusionType;
import org.triplea.ai.flowfield.map.ResourceValue;

/**
 * Finds all of the un-owned territories that have a resource and assigns them a value and diffusion
 * rate
 */
@UtilityClass
public class ResourceToGet {

  public static DiffusionType build(
      final GamePlayer gamePlayer,
      final RelationshipTracker relationshipTracker,
      final GameMap gameMap,
      final Resource resource) {
    final Collection<GamePlayer> allies = relationshipTracker.getAllies(gamePlayer, true);
    final Map<Territory, Long> territoryValuations =
        gameMap.getTerritories().stream()
            .filter(Predicate.not(territory -> allies.contains(territory.getOwner())))
            .collect(
                Collectors.toMap(
                    Function.identity(), ResourceValue.mapTerritoryToResource(resource)));
    return new DiffusionType(
        "Resource (" + resource.getName() + ") To Get", 0.65, territoryValuations);
  }
}
