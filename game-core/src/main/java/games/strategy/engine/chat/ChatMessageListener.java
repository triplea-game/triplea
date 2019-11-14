package games.strategy.engine.chat;

import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.events.server.ChatEvent;
import org.triplea.http.client.lobby.chat.events.server.ChatMessage;

/** Callback interface for a component that is interested in chat messages. */
public interface ChatMessageListener {
  void eventReceived(ChatEvent eventText);

  void messageReceived(ChatMessage chatMessage);

  void slapped(String message, PlayerName from);

  void slap(String message);

  void playerJoined(String message);

  void playerLeft(String message);
}
