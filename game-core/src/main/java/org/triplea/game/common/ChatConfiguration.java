package org.triplea.game.common;

import games.strategy.engine.chat.Chat;

public interface ChatConfiguration {

  void setChat(final Chat chat);

  Chat getChat();

  String getAllText();

  void setShowChatTime(final boolean showTime);
}
