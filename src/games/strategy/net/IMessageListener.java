package games.strategy.net;

import java.io.Serializable;

public interface IMessageListener {
  void messageReceived(Serializable msg, INode from);
}
