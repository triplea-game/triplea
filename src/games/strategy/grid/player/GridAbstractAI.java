package games.strategy.grid.player;

import java.util.Collection;

import games.strategy.common.player.AbstractBaseAI;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;

/**
 * Abstract class for a Grid AI agent.
 */
public abstract class GridAbstractAI extends AbstractBaseAI implements IGridGamePlayer {
  public GridAbstractAI(final String name, final String type) {
    super(name, type);
  }

  protected abstract void play();

  protected void endTurn() {}

  /**
   * The given phase has started. Parse the phase name and call the appropiate method.
   */
  @Override
  public void start(final String stepName) {
    super.start(stepName); // must call super.start
    if (stepName.endsWith("Play")) {
      play();
    } else if (stepName.endsWith("EndTurn")) {
      endTurn();
    } else {
      throw new IllegalArgumentException("Unrecognized step stepName:" + stepName);
    }
  }

  public final Class<IGridGamePlayer> getRemotePlayerType() {
    return IGridGamePlayer.class;
  }

  @Override
  public UnitType selectUnit(final Unit startUnit, final Collection<UnitType> options, final Territory territory,
      final PlayerID player, final GameData data, final String message) {
    return null;
  }
}
