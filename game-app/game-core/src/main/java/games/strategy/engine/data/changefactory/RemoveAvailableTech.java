package games.strategy.engine.data.changefactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.engine.data.TechnologyFrontierList.getTechnologyFrontierOrThrow;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.triplea.delegate.TechAdvance;

class RemoveAvailableTech extends Change {
  private static final long serialVersionUID = 6131447662760022521L;

  private final TechAdvance tech;
  private final TechnologyFrontier frontier;
  private final GamePlayer player;

  RemoveAvailableTech(
      final TechnologyFrontier front, final TechAdvance tech, final GamePlayer player) {
    checkNotNull(front);
    checkNotNull(tech);

    this.tech = tech;
    frontier = front;
    this.player = player;
  }

  @Override
  public void perform(final GameState data) {
    getTechnologyFrontierOrThrow(player, frontier.getName()).removeAdvance(tech);
  }

  @Override
  public Change invert() {
    return new AddAvailableTech(frontier, tech, player);
  }
}
