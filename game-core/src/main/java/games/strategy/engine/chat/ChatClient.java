package games.strategy.engine.chat;

import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.http.client.lobby.chat.messages.server.ChatMessage;
import org.triplea.http.client.lobby.chat.messages.server.ChatterList;
import org.triplea.http.client.lobby.chat.messages.server.StatusUpdate;

/**
 * ChatClient can also be thought of as a 'ChatListener' (it is not named so to avoid confusion with
 * {@code IChatListener}). This API is called when the server 'pushes' messages to the local client.
 *
 * <p>In other words, instances of this interface can be registered with a chat connection and the
 * API methods defined here are then used as callbacks, they are called by the server to notify the
 * local client of chat events.
 */
public interface ChatClient {
  /**
   * Initial (async) connection to server is established. Not invoked for synchronous connections.
   */
  void connected(ChatterList chatters);

  /** A chat message has been received. */
  void messageReceived(ChatMessage chatMessage);

  void eventReceived(String chatEvent);

  /**
   * A new chatter has joined.
   *
   * @param chatParticipant The newly joined chatter.
   */
  void participantAdded(ChatParticipant chatParticipant);

  /** A chatter has left chat. */
  void participantRemoved(PlayerName playerName);

  /**
   * This method being called indicates the current player has been slapped.
   *
   * @param slapper The player that issued the slap.
   */
  void slappedBy(PlayerName slapper);

  /** A message that notifies players that another player has been slapped, eg: "x slapped y". */
  void playerSlapped(String eventMessage);

  /** Indicates a players status has changed. */
  void statusUpdated(StatusUpdate statusUpdate);
}
