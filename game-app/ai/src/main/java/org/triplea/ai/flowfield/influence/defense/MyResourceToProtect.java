package org.triplea.ai.flowfield.influence.defense;

import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.Matches;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.triplea.ai.flowfield.influence.InfluenceMapSetup;
import org.triplea.ai.flowfield.influence.ResourceValue;

@UtilityClass
public class MyResourceToProtect {

  public static InfluenceMapSetup build(
      final GamePlayer gamePlayer, final GameMap gameMap, final Resource resource) {
    final Map<Territory, Long> territoryValuations =
        gameMap.getTerritories().stream()
            .filter(Matches.isTerritoryOwnedBy(gamePlayer))
            .collect(
                Collectors.toMap(
                    Function.identity(), ResourceValue.territoryToResourceValue(resource)));
    return new InfluenceMapSetup(
        "My Resource (" + resource.getName() + ") to Protect", 0.25, territoryValuations);
  }
}
