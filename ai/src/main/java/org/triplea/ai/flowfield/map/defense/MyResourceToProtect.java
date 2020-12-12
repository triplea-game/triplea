package org.triplea.ai.flowfield.map.defense;

import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.triplea.ai.flowfield.map.DiffusionType;
import org.triplea.ai.flowfield.map.ResourceValue;

@UtilityClass
public class MyResourceToProtect {

  public static DiffusionType build(
      final GamePlayer gamePlayer, final GameMap gameMap, final Resource resource) {
    final Map<Territory, Long> territoryValuations =
        gameMap.getTerritories().stream()
            .filter(territory -> territory.getOwner().equals(gamePlayer))
            .collect(
                Collectors.toMap(
                    Function.identity(), ResourceValue.mapTerritoryToResource(resource)));
    return new DiffusionType(
        "My Resource (" + resource.getName() + ") to Protect", 0.70, territoryValuations);
  }
}
