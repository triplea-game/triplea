package org.triplea.ai.flowfield.influence.offense;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.triplea.ai.flowfield.influence.InfluenceMapSetup;
import org.triplea.ai.flowfield.influence.ResourceValue;

/**
 * Finds all of the un-owned territories that have a resource and assigns them a value and diffusion
 * rate
 */
@UtilityClass
public class ResourceToGet {

  public static InfluenceMapSetup build(
      final GamePlayer gamePlayer,
      final RelationshipTracker relationshipTracker,
      final Collection<Territory> territories,
      final Resource resource) {
    final Collection<GamePlayer> enemies =
        new ArrayList<>(List.of(gamePlayer.getData().getPlayerList().getNullPlayer()));
    enemies.addAll(relationshipTracker.getEnemies(gamePlayer));

    final Map<Territory, Long> territoryValuations =
        territories.stream()
            .filter(territory -> enemies.contains(territory.getOwner()))
            .collect(
                Collectors.toMap(
                    Function.identity(), ResourceValue.territoryToResourceValue(resource)));
    return new InfluenceMapSetup(
        "Resource (" + resource.getName() + ") To Get", 0.30, territoryValuations);
  }
}
