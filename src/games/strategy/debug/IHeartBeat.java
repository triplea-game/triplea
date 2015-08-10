package games.strategy.debug;

import games.strategy.engine.message.IRemote;

public interface IHeartBeat extends IRemote {
  public String getDebugInfo();
}
