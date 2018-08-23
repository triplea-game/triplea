package games.strategy.engine.framework.startup.mc;

import java.io.IOException;

import games.strategy.engine.chat.AdministrativeChatMessages;
import games.strategy.net.AbstractServerMessenger;
import games.strategy.net.IObjectStreamFactory;

final class GameServerMessenger extends AbstractServerMessenger {
  GameServerMessenger(final String name, final int port, final IObjectStreamFactory objectStreamFactory)
      throws IOException {
    super(name, port, objectStreamFactory);
  }

  @Override
  protected String getAdministrativeMuteChatMessage() {
    return AdministrativeChatMessages.YOU_HAVE_BEEN_MUTED_GAME;
  }

  @Override
  protected String getChatChannelName() {
    return ServerModel.CHAT_NAME;
  }
}
