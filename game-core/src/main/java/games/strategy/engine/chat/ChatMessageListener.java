package games.strategy.engine.chat;

import games.strategy.engine.lobby.PlayerName;

/** Callback interface for a component that is interested in chat messages. */
public interface ChatMessageListener {
  void addMessage(String message, PlayerName from);

  void addSlapMessage(String message, PlayerName from);

  void addStatusMessage(String message, boolean joined);
}
