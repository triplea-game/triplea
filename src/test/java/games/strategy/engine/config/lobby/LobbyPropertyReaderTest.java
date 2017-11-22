package games.strategy.engine.config.lobby;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import games.strategy.engine.config.MemoryPropertyReader;
import games.strategy.engine.config.lobby.LobbyPropertyReader.PropertyKeys;

public class LobbyPropertyReaderTest {
  private static LobbyPropertyReader newLobbyPropertyReader(final String key, final String value) {
    return new LobbyPropertyReader(new MemoryPropertyReader(Collections.singletonMap(key, value)));
  }

  @Test
  public void getPort() {
    final int value = 100;
    final LobbyPropertyReader lobbyPropertyReader = newLobbyPropertyReader(PropertyKeys.PORT, String.valueOf(value));

    assertThat(lobbyPropertyReader.getPort(), is(value));
  }

  @Test
  public void postgresUser() {
    final String value = "funnyName";
    final LobbyPropertyReader lobbyPropertyReader = newLobbyPropertyReader(PropertyKeys.POSTGRES_USER, value);

    assertThat(lobbyPropertyReader.getPostgresUser(), is(value));
  }

  @Test
  public void postgresPassword() {
    final String value = "funnyPasssword";
    final LobbyPropertyReader lobbyPropertyReader = newLobbyPropertyReader(PropertyKeys.POSTGRES_PASSWORD, value);

    assertThat(lobbyPropertyReader.getPostgresPassword(), is(value));
  }

  @Test
  public void isMaintenanceMode_ShouldReturnTrueWhenMaintenanceModeEnabled() {
    Arrays.asList("true", "TRUE")
        .forEach(value -> {
          final LobbyPropertyReader lobbyPropertyReader = newLobbyPropertyReader(PropertyKeys.MAINTENANCE_MODE, value);
          assertThat(lobbyPropertyReader.isMaintenanceMode(), is(true));
        });
  }

  @Test
  public void isMaintenanceMode_ShouldReturnFalseWhenMaintenanceModeDisabled() {
    Arrays.asList("", "false", "FALSE", "other")
        .forEach(value -> {
          final LobbyPropertyReader lobbyPropertyReader = newLobbyPropertyReader(PropertyKeys.MAINTENANCE_MODE, value);
          assertThat(lobbyPropertyReader.isMaintenanceMode(), is(false));
        });
  }
}
