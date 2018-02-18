package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.triplea.delegate.TechAdvance;

class AddAvailableTech extends Change {
  private static final long serialVersionUID = 5664428883866434959L;
  private final TechAdvance m_tech;
  private final TechnologyFrontier m_frontier;
  private final PlayerID m_player;

  public AddAvailableTech(final TechnologyFrontier front, final TechAdvance tech, final PlayerID player) {
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
    front.addAdvance(m_tech);
  }

  @Override
  public Change invert() {
    return new RemoveAvailableTech(m_frontier, m_tech, m_player);
  }
}
