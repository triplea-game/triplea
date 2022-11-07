package org.triplea.ai.flowfield.influence.offense;

import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.attachments.TerritoryAttachment;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.triplea.ai.flowfield.influence.InfluenceMapSetup;

/** Finds all of the enemy capitals and assigns them a value and diffusion rate */
@UtilityClass
public class EnemyCapitals {

  public static InfluenceMapSetup build(
      final GamePlayer gamePlayer, final PlayerList playerList, final GameMap gameMap) {
    final Map<Territory, Long> territoryValuations =
        playerList.getPlayers().stream()
            .filter(player -> !player.equals(gamePlayer))
            .map(player -> TerritoryAttachment.getAllCapitals(player, gameMap))
            .flatMap(Collection::stream)
            .collect(Collectors.toMap(Function.identity(), territory -> 100L));
    return new InfluenceMapSetup("Other Capitals", 0.70, territoryValuations);
  }
}
