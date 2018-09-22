package org.triplea.lobby.server;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import org.triplea.lobby.common.LobbyConstants;
import org.triplea.lobby.server.config.LobbyConfiguration;
import org.triplea.lobby.server.db.Database;
import org.triplea.lobby.server.db.MutedMacController;
import org.triplea.lobby.server.db.MutedUsernameController;

import games.strategy.engine.chat.AdministrativeChatMessages;
import games.strategy.net.AbstractServerMessenger;
import games.strategy.net.DefaultObjectStreamFactory;

final class LobbyServerMessenger extends AbstractServerMessenger {
  private final MutedMacController mutedMacController;
  private final MutedUsernameController mutedUsernameController;

  LobbyServerMessenger(final String name, final LobbyConfiguration lobbyConfiguration) throws IOException {
    super(name, lobbyConfiguration.getPort(), new DefaultObjectStreamFactory());

    final Database database = new Database(lobbyConfiguration);
    mutedMacController = new MutedMacController(database);
    mutedUsernameController = new MutedUsernameController(database);
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
    return mutedMacController.getMacUnmuteTime(mac);
  }

  @Override
  protected Optional<Instant> getUsernameUnmuteTime(final String username) {
    return mutedUsernameController.getUsernameUnmuteTime(username);
  }

  @Override
  protected boolean isMacMutedInBackingStore(final String mac) {
    return mutedMacController.isMacMuted(mac);
  }

  @Override
  protected boolean isUsernameMutedInBackingStore(final String username) {
    return mutedUsernameController.isUsernameMuted(username);
  }
}
