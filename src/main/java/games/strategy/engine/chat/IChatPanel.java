package games.strategy.engine.chat;

import javax.swing.DefaultListCellRenderer;

/**
 * Not sure if this is the right way to go about it, but we need a headless version, so I'm making an interface so we
 * can use the headless
 * or non-headless versions as we like.
 */
public interface IChatPanel {
  boolean isHeadless();

  void shutDown();

  void setChat(final Chat chat);

  Chat getChat();

  void setPlayerRenderer(final DefaultListCellRenderer renderer);

  void setShowChatTime(final boolean showTime);

  String getAllText();
}
