package games.strategy.net;

import java.io.Serializable;

public interface IMessageListener {
  public void messageReceived(Serializable msg, INode from);
}
