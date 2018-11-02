package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.triplea.delegate.TechAdvance;

class RemoveAvailableTech extends Change {
  private static final long serialVersionUID = 6131447662760022521L;

  private final TechAdvance tech;
  private final TechnologyFrontier frontier;
  private final PlayerID player;

  public RemoveAvailableTech(final TechnologyFrontier front, final TechAdvance tech, final PlayerID player) {
    if (front == null) {
      throw new IllegalArgumentException("Null tech category");
    }
    if (tech == null) {
      throw new IllegalArgumentException("Null tech");
    }
    this.tech = tech;
    frontier = front;
    this.player = player;
  }

  @Override
  public void perform(final GameData data) {
    final TechnologyFrontier front = player.getTechnologyFrontierList().getTechnologyFrontier(frontier.getName());
    front.removeAdvance(tech);
  }

  @Override
  public Change invert() {
    return new AddAvailableTech(frontier, tech, player);
  }
}
