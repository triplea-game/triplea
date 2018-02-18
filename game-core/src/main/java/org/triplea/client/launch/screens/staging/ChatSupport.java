package org.triplea.client.launch.screens.staging;

import java.io.IOException;

import javax.swing.JPanel;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.framework.startup.login.ClientLogin;
import games.strategy.engine.framework.startup.mc.ServerConnectionProps;
import games.strategy.engine.message.ChannelMessenger;
import games.strategy.engine.message.RemoteMessenger;
import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;
import games.strategy.net.ClientMessenger;

/**
 * Wrapper around 'ChatPanel' to improve API usage.
 */
// TODO: fix codesmell, ChatPanel has UI + chat network logic mixed together.
public class ChatSupport {
  private static final String CHAT_NAME = "games.strategy.engine.framework.ui.ServerStartup.CHAT_NAME";

  private final ChatPanel chatPanel;

  // server case
  public ChatSupport(final ChatPanel chatPanel) {
    this.chatPanel = chatPanel;
  }

  // client case
  public ChatSupport(final ServerConnectionProps props) {
    try {
      final ClientMessenger clientMessenger = new ClientMessenger(
          props.getHost(),
          props.getPort(),
          "name",
          "$1$MH$7890123456789012345678",
          new ClientLogin(null));
      final UnifiedMessenger unifiedMessenger = new UnifiedMessenger(clientMessenger);
      final RemoteMessenger remoteMessenger = new RemoteMessenger(unifiedMessenger);
      final ChannelMessenger channelMessenger = new ChannelMessenger(unifiedMessenger);

      chatPanel = new ChatPanel(clientMessenger, channelMessenger, remoteMessenger, CHAT_NAME,
          Chat.ChatSoundProfile.GAME_CHATROOM);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  public JPanel getChatPanel() {
    return chatPanel;
  }
}
