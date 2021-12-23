package games.strategy.engine.stats;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.triplea.ui.mapdata.MapData;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResourceStat implements IStat {
  public final Resource resource;

  @Override
  public String getName() {
    return resource == null ? "" : resource.getName();
  }

  @Override
  public double getValue(final GamePlayer player, final GameData data, final MapData mapData) {
    return player.getResources().getQuantity(resource);
  }
}
