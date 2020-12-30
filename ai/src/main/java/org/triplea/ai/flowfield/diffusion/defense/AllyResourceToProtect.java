package org.triplea.ai.flowfield.diffusion.defense;

import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.triplea.ai.flowfield.diffusion.DiffusionType;
import org.triplea.ai.flowfield.diffusion.ResourceValue;

@UtilityClass
public class AllyResourceToProtect {

  public static DiffusionType build(
      final GamePlayer gamePlayer,
      final RelationshipTracker relationshipTracker,
      final GameMap gameMap,
      final Resource resource) {
    final Collection<GamePlayer> allies = relationshipTracker.getAllies(gamePlayer, false);
    final Map<Territory, Long> territoryValuations =
        gameMap.getTerritories().stream()
            .filter(territory -> allies.contains(territory.getOwner()))
            .collect(
                Collectors.toMap(
                    Function.identity(), ResourceValue.territoryToResourceValue(resource)));
    return new DiffusionType(
        "Ally Resource (" + resource.getName() + ") to Protect", 0.20, territoryValuations);
  }
}
