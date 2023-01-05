package org.triplea.modules.user.account.login;

import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import org.jdbi.v3.core.Jdbi;

/** Fetches from database the lobby login message */
@AllArgsConstructor
public class LobbyLoginMessageDao implements Supplier<String> {

  private final Jdbi jdbi;

  static LobbyLoginMessageDao build(final Jdbi jdbi) {
    return new LobbyLoginMessageDao(jdbi);
  }

  @Override
  public String get() {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("select message from lobby_message") //
                .mapTo(String.class)
                .one());
  }
}
