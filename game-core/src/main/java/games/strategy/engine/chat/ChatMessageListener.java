package games.strategy.engine.chat;

import games.strategy.engine.lobby.PlayerName;

/** Callback interface for a component that is interested in chat messages. */
public interface ChatMessageListener {
  void addMessage(String message, PlayerName from);

  void addMessageWithSound(String message, PlayerName from, String sound);

  void addStatusMessage(String message);
}
