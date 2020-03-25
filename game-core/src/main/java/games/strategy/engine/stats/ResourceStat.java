package games.strategy.engine.stats;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResourceStat extends AbstractStat {
  public final Resource resource;

  @Override
  public String getName() {
    return resource == null ? "" : resource.getName();
  }

  @Override
  public double getValue(final GamePlayer player, final GameData data) {
    return player.getResources().getQuantity(resource);
  }
}
