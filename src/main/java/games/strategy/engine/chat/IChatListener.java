package games.strategy.engine.chat;

import java.util.Collection;

import games.strategy.net.INode;

/**
 * An interface to allow for testing.
 */
public interface IChatListener {
  void updatePlayerList(final Collection<INode> players);

  void addMessage(final String message, final String from, final boolean thirdperson);

  void addMessageWithSound(final String message, final String from, final boolean thirdperson,
      final String sound);

  void addStatusMessage(final String message);
}
