package games.strategy.engine.chat;

import java.util.Collection;

import games.strategy.net.INode;

/**
 * An interface to allow for testing.
 */
public interface IChatListener {
  public void updatePlayerList(final Collection<INode> players);

  public void addMessage(final String message, final String from, final boolean thirdperson);

  public void addMessageWithSound(final String message, final String from, final boolean thirdperson,
      final String sound);

  public void addStatusMessage(final String message);
}
