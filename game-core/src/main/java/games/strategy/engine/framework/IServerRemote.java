package games.strategy.engine.framework;

import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;

interface IServerRemote extends IRemote {
  @RemoteActionCode(0)
  byte[] getSavedGame();
}
