package games.strategy.grid.player;

import java.util.Collection;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.gamePlayer.IRemotePlayer;

/**
 * Interface which all Grid Game players classes must implement.
 */
public interface IGridGamePlayer extends IRemotePlayer {
  public UnitType selectUnit(final Unit startUnit, final Collection<UnitType> options, final Territory territory,
      final PlayerID player, final GameData data, final String message);
}
