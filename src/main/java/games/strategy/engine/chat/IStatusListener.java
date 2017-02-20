package games.strategy.engine.chat;

import games.strategy.net.INode;

public interface IStatusListener {
  void statusChanged(INode node, String newStatus);
}
