package org.triplea.ai.flowfield.influence;

import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.ResourceList;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.experimental.FieldDefaults;
import org.triplea.ai.flowfield.influence.offense.EnemyCapitals;
import org.triplea.ai.flowfield.influence.offense.ResourceToGet;
import org.triplea.ai.flowfield.neighbors.MapWithNeighbors;
import org.triplea.ai.flowfield.odds.BattleDetails;

@Builder(builderMethodName = "setup")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class InfluenceMapBuilder {
  public static final @Nonnull String RELATIONSHIP_ALLY = "ALLY";
  public static final @Nonnull String RELATIONSHIP_UNKNOWN = "UNKNOWN";
  public static final @Nonnull String RELATIONSHIP_ENEMY = "ENEMY";
  @Nonnull GamePlayer gamePlayer;
  @Nonnull PlayerList playerList;
  @Nonnull ResourceList resourceList;
  @Nonnull RelationshipTracker relationshipTracker;
  @Nonnull GameMap gameMap;
  @Nonnull CombatValueBuilder.MainBuilder offenseCombatBuilder;
  @Nonnull CombatValueBuilder.MainBuilder defenseCombatBuilder;

  public Collection<InfluenceMap> buildMaps(
      final String mapGroup, final MapWithNeighbors mapWithNeighbors) {
    return Stream.of(buildCombatMaps(mapGroup, mapWithNeighbors))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  public Collection<InfluenceMap> buildCombatMaps(
      final String mapGroup, final MapWithNeighbors mapWithNeighbors) {
    final Collection<InfluenceMap> combatMaps = new ArrayList<>();

    combatMaps.addAll(
        resourceList.getResources().stream()
            .map(
                resource ->
                    new InfluenceMap(
                        mapGroup,
                        ResourceToGet.build(
                            gamePlayer, relationshipTracker, gameMap.getTerritories(), resource),
                        mapWithNeighbors))
            .collect(Collectors.toList()));

    combatMaps.addAll(
        EnemyCapitals.build(gamePlayer, playerList, gameMap).splitIntoSingleTerritoryMaps().stream()
            .map(
                influenceMapSetup ->
                    new InfluenceMap(
                        mapGroup, influenceMapSetup, mapWithNeighbors, getBattleDetails()))
            .collect(Collectors.toList()));
    return combatMaps;
  }

  private Function<Territory, BattleDetails> getBattleDetails() {
    final Collection<GamePlayer> allies = relationshipTracker.getAllies(gamePlayer, true);
    final Collection<GamePlayer> enemies = relationshipTracker.getEnemies(gamePlayer);
    return territory -> {
      final Map<String, List<Unit>> unitsGroupedByRelationship =
          territory.getUnits().stream()
              .collect(
                  Collectors.groupingBy(
                      unit -> {
                        if (allies.contains(unit.getOwner())) {
                          return RELATIONSHIP_ALLY;
                        } else if (enemies.contains(unit.getOwner())) {
                          return RELATIONSHIP_ENEMY;
                        } else {
                          return RELATIONSHIP_UNKNOWN;
                        }
                      }));
      if (!unitsGroupedByRelationship.containsKey(RELATIONSHIP_ALLY)
          && !unitsGroupedByRelationship.containsKey(RELATIONSHIP_ENEMY)) {
        return BattleDetails.EMPTY_DETAILS;
      }
      return new BattleDetails(
          unitsGroupedByRelationship.getOrDefault(RELATIONSHIP_ALLY, List.of()),
          unitsGroupedByRelationship.getOrDefault(RELATIONSHIP_ENEMY, List.of()),
          offenseCombatBuilder,
          defenseCombatBuilder,
          TerritoryAttachment.get(territory)
              .map(TerritoryAttachment::getTerritoryEffect)
              .orElse(List.of()));
    };
  }
}
