package games.strategy.engine.framework;

import games.strategy.engine.message.IRemote;

interface IServerRemote extends IRemote {
  byte[] getSavedGame();
}
