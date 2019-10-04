package games.strategy.engine.chat;

import games.strategy.engine.lobby.PlayerName;
import java.util.Map;

/**
 * Interface to represent 'send' actions from a chat client to server. This interface can be used to
 * push messages to the server.
 */
public interface ChatTransmitter {
  /**
   * Connects to chat, adds current player and returns the set of players that are in chat mapped to
   * their status.
   */
  Map<ChatParticipant, String> connect();

  /** Disconnects current player from chat. */
  void disconnect();

  /** Sends a message from current player to chat. */
  void sendMessage(String message);

  /** Sends a slap to a target player. */
  void slap(PlayerName playerName);

  /** Updates the status of current player. */
  void updateStatus(String status);
}
