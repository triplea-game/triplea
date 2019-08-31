package org.triplea.lobby.server.login;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.triplea.lobby.common.login.RsaAuthenticator;
import org.triplea.lobby.server.config.LobbyConfiguration;
import org.triplea.lobby.server.db.JdbiDatabase;
import org.triplea.lobby.server.db.dao.TempPasswordDao;
import org.triplea.lobby.server.login.forgot.password.verify.TempPasswordVerification;

/** Factory class to construct {@code LobbyLoginValidator}. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LobbyLoginValidatorFactory {

  /** Factory method to construct {@code LobbyLoginValidator}. */
  public static LobbyLoginValidator newLobbyLoginValidator(
      final LobbyConfiguration lobbyConfiguration) {

    final var tempPasswordDao = JdbiDatabase.newConnection().onDemand(TempPasswordDao.class);

    return new LobbyLoginValidator(
        lobbyConfiguration.getDatabaseDao(),
        new RsaAuthenticator(),
        BCrypt::gensalt,
        new FailedLoginThrottle(),
        new TempPasswordVerification(tempPasswordDao),
        new AllowLoginRules(lobbyConfiguration.getDatabaseDao()),
        new AllowCreateUserRules(lobbyConfiguration.getDatabaseDao()));
  }
}
