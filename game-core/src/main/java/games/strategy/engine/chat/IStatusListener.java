package games.strategy.engine.chat;

import games.strategy.net.INode;

/**
 * A listener that is notified of changes to the status of all chat participants.
 */
public interface IStatusListener {
  void statusChanged(INode node, String newStatus);
}
