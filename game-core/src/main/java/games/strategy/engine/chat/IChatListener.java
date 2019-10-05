package games.strategy.engine.chat;

import games.strategy.engine.lobby.PlayerName;
import java.util.Collection;

/** An interface to allow for testing. */
public interface IChatListener {
  void updatePlayerList(Collection<ChatParticipant> players);

  void addMessage(String message, PlayerName from);

  void addMessageWithSound(String message, PlayerName from, String sound);

  void addStatusMessage(String message);
}
