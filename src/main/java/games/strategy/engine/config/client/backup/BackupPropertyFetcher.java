package games.strategy.engine.config.client.backup;

import games.strategy.engine.lobby.client.login.LobbyServerProperties;

/*
 * TODO: We should store the last value and use that if we can connect to github.
 *       We currently have the problem that the lobby URL is stored in two places,
 *       in the backup configuration, and again in the live configuration. If we cache
 *       and default back to that value, then we potentially would not need to include
 *       it in game_engine.properties at all.
 */
public class BackupPropertyFetcher {

  /**
   * Parses a backup property config from game_engine.properties that gives us the lobby IP and port.
   */
  public LobbyServerProperties parseBackupValuesFromEngineConfig(final String backupLobbyUrl) {
    if (backupLobbyUrl == null || !backupLobbyUrl.contains(":")) {
      throw new InvalidLobbyAddressException(backupLobbyUrl);
    }

    final String url = backupLobbyUrl.substring(0, backupLobbyUrl.indexOf(":"));
    if (url.isEmpty()) {
      throw new InvalidLobbyAddressException(backupLobbyUrl);
    }


    final String port = backupLobbyUrl.substring(backupLobbyUrl.indexOf(":") + 1, backupLobbyUrl.length());

    try {
      final int portNumber = Integer.parseInt(port);
      return new LobbyServerProperties(url, portNumber);
    } catch (final NumberFormatException e) {
      throw new InvalidLobbyAddressException(backupLobbyUrl);
    }
  }


  static class InvalidLobbyAddressException extends IllegalArgumentException {
    InvalidLobbyAddressException(final String invalidValue) {
      super("There is a problem with configuration value '" + invalidValue + "', it does not seem to "
          + "be in a '<host>:<port>' format.");
    }
  }
}
