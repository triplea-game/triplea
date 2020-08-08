package games.strategy.engine.chat;

import org.triplea.domain.data.UserName;

/** Callback interface for a component that is interested in chat messages. */
public interface ChatMessageListener {
  void eventReceived(String eventText);

  void messageReceived(UserName fromPlayer, String chatMessage);

  void slapped(UserName from);

  void playerJoined(String message);

  void playerLeft(String message);
}
