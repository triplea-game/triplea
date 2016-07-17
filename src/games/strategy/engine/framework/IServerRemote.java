package games.strategy.engine.framework;

import games.strategy.engine.message.IRemote;

public interface IServerRemote extends IRemote {
  byte[] getSavedGame();
}
