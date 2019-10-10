package games.strategy.engine.chat;

import org.triplea.domain.data.PlayerName;

/** Callback interface for a component that is interested in chat messages. */
public interface ChatMessageListener {
  void messageReceived(String message, PlayerName from);

  void slapped(String message, PlayerName from);

  void slap(String message);

  void playerJoined(String message);

  void playerLeft(String message);
}
