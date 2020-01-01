package games.strategy.engine.chat;

import java.util.Collection;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.chat.ChatParticipant;

/**
 * Interface to represent 'send' actions from a chat client to server. This interface can be used to
 * push messages to the server.
 */
public interface ChatTransmitter {
  void setChatClient(ChatClient chatClient);

  /** Connects to chat, adds current player and returns the set of players that are in chat. */
  Collection<ChatParticipant> connect();

  /** Disconnects current player from chat. */
  void disconnect();

  /** Sends a message from current player to chat. */
  void sendMessage(String message);

  /** Sends a slap to a target player. */
  void slap(UserName userName);

  /** Updates the status of current player. */
  void updateStatus(String status);

  UserName getLocalUserName();
}
