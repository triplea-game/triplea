package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.triplea.delegate.TechAdvance;

class RemoveAvailableTech extends Change {
  private static final long serialVersionUID = 6131447662760022521L;
  private final TechAdvance m_tech;
  private final TechnologyFrontier m_frontier;
  private final PlayerID m_player;

  public RemoveAvailableTech(final TechnologyFrontier front, final TechAdvance tech, final PlayerID player) {
    if (front == null) {
      throw new IllegalArgumentException("Null tech category");
    }
    if (tech == null) {
      throw new IllegalArgumentException("Null tech");
    }
    m_tech = tech;
    m_frontier = front;
    m_player = player;
  }

  @Override
  public void perform(final GameData data) {
    final TechnologyFrontier front = m_player.getTechnologyFrontierList().getTechnologyFrontier(m_frontier.getName());
    front.removeAdvance(m_tech);
  }

  @Override
  public Change invert() {
    return new AddAvailableTech(m_frontier, m_tech, m_player);
  }
}
