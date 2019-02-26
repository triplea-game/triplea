package games.strategy.engine.chat;

import java.util.Collection;

import games.strategy.net.INode;

/**
 * An interface to allow for testing.
 */
public interface IChatListener {
  void updatePlayerList(Collection<INode> players);

  void addMessage(String message, String from, boolean thirdperson);

  void addMessageWithSound(String message, String from, boolean thirdperson, String sound);

  void addStatusMessage(String message);
}
