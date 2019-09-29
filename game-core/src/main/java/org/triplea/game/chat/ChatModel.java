package org.triplea.game.chat;

import games.strategy.engine.chat.Chat;
import java.awt.Component;
import java.util.Optional;

/**
 * Interface to abstract common functionality to configure a chat that is shared between headed and
 * headless implementations.
 */
public interface ChatModel {

  Chat getChat();

  String getAllText();

  Optional<Component> getViewComponent();
}
