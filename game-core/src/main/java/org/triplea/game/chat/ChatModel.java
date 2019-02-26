package org.triplea.game.chat;

import games.strategy.engine.chat.Chat;

/**
 * Interface to abstract common functionality to configure a chat
 * that is shared between headed and headless implementations.
 */
public interface ChatModel {

  void setChat(Chat chat);

  Chat getChat();

  String getAllText();

  void setShowChatTime(boolean showTime);
}
