package games.strategy.engine.data.changefactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.engine.data.TechnologyFrontierList.getTechnologyFrontierOrThrow;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.triplea.delegate.TechAdvance;

class AddAvailableTech extends Change {
  private static final long serialVersionUID = 5664428883866434959L;

  private final TechAdvance tech;
  private final TechnologyFrontier frontier;
  private final GamePlayer player;

  AddAvailableTech(
      final TechnologyFrontier front, final TechAdvance tech, final GamePlayer player) {
    checkNotNull(front);
    checkNotNull(tech);

    this.tech = tech;
    frontier = front;
    this.player = player;
  }

  @Override
  public void perform(final GameState data) {
    getTechnologyFrontierOrThrow(player, frontier.getName()).addAdvance(tech);
  }

  @Override
  public Change invert() {
    return new RemoveAvailableTech(frontier, tech, player);
  }
}
