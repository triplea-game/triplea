package org.triplea.ai.flowfield.diffusion;

import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceList;
import games.strategy.triplea.Constants;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.triplea.ai.flowfield.diffusion.offense.EnemyCapitals;
import org.triplea.ai.flowfield.diffusion.offense.ResourceToGet;
import org.triplea.ai.flowfield.neighbors.MapWithNeighbors;

@Builder(builderMethodName = "setup")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class DiffusionMapBuilder {
  @NonNull GamePlayer gamePlayer;
  @NonNull PlayerList playerList;
  @NonNull ResourceList resourceList;
  @NonNull RelationshipTracker relationshipTracker;
  @NonNull GameMap gameMap;

  public Collection<DiffusionMap> buildMaps(
      final String mapGroup, final MapWithNeighbors mapWithNeighbors) {
    return Stream.of(buildCombatMaps(mapGroup, mapWithNeighbors))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  public Collection<DiffusionMap> buildCombatMaps(
      final String mapGroup, final MapWithNeighbors mapWithNeighbors) {
    final Resource pus = resourceList.getResource(Constants.PUS);
    return List.of(
            EnemyCapitals.build(gamePlayer, playerList, gameMap),
            ResourceToGet.build(gamePlayer, relationshipTracker, gameMap.getTerritories(), pus))
        .stream()
        .map(diffusionType -> new DiffusionMap(mapGroup, diffusionType, mapWithNeighbors))
        .collect(Collectors.toList());
  }
}
