package games.strategy.engine.chat;

import games.strategy.engine.lobby.PlayerName;

/**
 * ChatClient can also be thought of as a 'ChatListener' (it is not named so to avoid confusion with
 * {@code IChatListener}). This API is called when the server 'pushes' messages to the local client.
 *
 * <p>In other words, instances of this interface can be registered with a chat connection and the
 * API methods defined here are then used as callbacks, they are called by the server to notify the
 * local client of chat events.
 */
public interface ChatClient {
  /** A chat message has been received. */
  void messageReceived(String message);

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

  /**
   * An event message is a message from the server that is a non-chat message, eg: "x slapped y".
   */
  void playerSlapped(String eventMessage);

  /** Indicates a players status has changed. */
  void statusUpdated(PlayerName playerName, String status);
}
