package games.strategy.net;

import java.io.Serializable;

/** A listener that receives event notifications from a {@link IMessenger}. */
public interface IMessageListener {
  void messageReceived(Serializable msg, INode from);
}
