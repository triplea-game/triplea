package games.strategy.engine.chat;

import games.strategy.engine.lobby.PlayerName;

/** Callback interface for a component that is interested in chat messages. */
public interface ChatMessageListener {
  void messageReceived(String message, PlayerName from);

  void slap(String message, PlayerName from);

  void slap(String message);

  void playerJoined(String message);

  void playerLeft(String message);
}
