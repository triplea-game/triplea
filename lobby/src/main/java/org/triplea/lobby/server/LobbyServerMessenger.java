package org.triplea.lobby.server;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import org.triplea.lobby.common.LobbyConstants;
import org.triplea.lobby.server.config.LobbyConfiguration;
import org.triplea.lobby.server.db.DatabaseDao;

import games.strategy.engine.chat.AdministrativeChatMessages;
import games.strategy.net.AbstractServerMessenger;
import games.strategy.net.DefaultObjectStreamFactory;

final class LobbyServerMessenger extends AbstractServerMessenger {
  private final DatabaseDao databaseDao;

  LobbyServerMessenger(final String name, final LobbyConfiguration lobbyConfiguration) throws IOException {
    super(name, lobbyConfiguration.getPort(), new DefaultObjectStreamFactory());

    databaseDao = lobbyConfiguration.getDatabaseDao();
  }

  @Override
  protected String getAdministrativeMuteChatMessage() {
    return AdministrativeChatMessages.YOU_HAVE_BEEN_MUTED_LOBBY;
  }

  @Override
  protected String getChatChannelName() {
    return LobbyConstants.LOBBY_CHAT;
  }

  @Override
  protected Optional<Instant> getMacUnmuteTime(final String mac) {
    return databaseDao.getMutedMacDao().getMacUnmuteTime(mac);
  }

  @Override
  protected Optional<Instant> getUsernameUnmuteTime(final String username) {
    return databaseDao.getMutedUsernameDao().getUsernameUnmuteTime(username);
  }

  @Override
  protected boolean isMacMutedInBackingStore(final String mac) {
    return databaseDao.getMutedMacDao().isMacMuted(mac);
  }

  @Override
  protected boolean isUsernameMutedInBackingStore(final String username) {
    return databaseDao.getMutedUsernameDao().isUsernameMuted(username);
  }
}
