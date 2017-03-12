package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;

/**
 * Adds/removes resource from a player.
 */
class ChangeResourceChange extends Change {
  private static final long serialVersionUID = -2304294240555842126L;
  private final String m_player;
  private final String m_resource;
  private final int m_quantity;

  ChangeResourceChange(final PlayerID player, final Resource resource, final int quantity) {
    m_player = player.getName();
    m_resource = resource.getName();
    m_quantity = quantity;
  }

  private ChangeResourceChange(final String player, final String resource, final int quantity) {
    m_player = player;
    m_resource = resource;
    m_quantity = quantity;
  }

  @Override
  public Change invert() {
    return new ChangeResourceChange(m_player, m_resource, -m_quantity);
  }

  @Override
  protected void perform(final GameData data) {
    final Resource resource = data.getResourceList().getResource(m_resource);
    final ResourceCollection resources = data.getPlayerList().getPlayerID(m_player).getResources();
    if (m_quantity > 0) {
      resources.addResource(resource, m_quantity);
    } else if (m_quantity < 0) {
      resources.removeResource(resource, -m_quantity);
    }
  }

  @Override
  public String toString() {
    return "Change resource.  Resource:" + m_resource + " quantity:" + m_quantity + " Player:" + m_player;
  }
}
