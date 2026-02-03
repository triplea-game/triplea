package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;

/** Adds/removes resource from a player. */
class ChangeResourceChange extends Change {
  private static final long serialVersionUID = -2304294240555842126L;

  private final String playerName;
  private final String resourceName;
  private final int quantity;

  ChangeResourceChange(final GamePlayer player, final Resource resource, final int quantity) {
    playerName = player.getName();
    resourceName = resource.getName();
    this.quantity = quantity;
  }

  private ChangeResourceChange(
      final String playerName, final String resourceName, final int quantity) {
    this.playerName = playerName;
    this.resourceName = resourceName;
    this.quantity = quantity;
  }

  @Override
  public Change invert() {
    return new ChangeResourceChange(playerName, resourceName, -quantity);
  }

  @Override
  protected void perform(final GameState data) {
    final Resource resource = data.getResourceList().getResourceOrThrow(resourceName);
    final ResourceCollection resources =
        data.getPlayerList().getPlayerId(playerName).getResources();
    if (quantity > 0) {
      resources.addResource(resource, quantity);
    } else if (quantity < 0) {
      resources.removeResourceUpTo(resource, -quantity);
    }
  }

  @Override
  public String toString() {
    return "Change resource.  Resource:"
        + resourceName
        + " quantity:"
        + quantity
        + " Player:"
        + playerName;
  }
}
