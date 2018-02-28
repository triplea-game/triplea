package games.strategy.engine.config.lobby;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import games.strategy.engine.config.MemoryPropertyReader;
import games.strategy.engine.config.lobby.LobbyPropertyReader.DefaultValues;
import games.strategy.engine.config.lobby.LobbyPropertyReader.PropertyKeys;

public final class LobbyPropertyReaderTest {
  private static LobbyPropertyReader newLobbyPropertyReader(final String key, final String value) {
    return new LobbyPropertyReader(new MemoryPropertyReader(Collections.singletonMap(key, value)));
  }

  @Nested
  public final class GetPortTest {
    @Test
    public void shouldReturnValueFromDelegateWhenPresent() {
      final int value = 100;
      final LobbyPropertyReader lobbyPropertyReader = newLobbyPropertyReader(PropertyKeys.PORT, String.valueOf(value));

      assertThat(lobbyPropertyReader.getPort(), is(value));
    }

    @Test
    public void shouldThrowExceptionWhenAbsent() {
      final LobbyPropertyReader lobbyPropertyReader = newLobbyPropertyReader(PropertyKeys.PORT, "");

      assertThrows(NumberFormatException.class, () -> lobbyPropertyReader.getPort());
    }

    @Test
    public void shouldThrowExceptionWhenMalformed() {
      final LobbyPropertyReader lobbyPropertyReader = newLobbyPropertyReader(PropertyKeys.PORT, "other");

      assertThrows(NumberFormatException.class, () -> lobbyPropertyReader.getPort());
    }
  }

  @Nested
  public final class GetPostgresDatabaseTest {
    @Test
    public void shouldReturnValueFromDelegateWhenPresent() {
      final String value = "database";
      final LobbyPropertyReader lobbyPropertyReader = newLobbyPropertyReader(PropertyKeys.POSTGRES_DATABASE, value);

      assertThat(lobbyPropertyReader.getPostgresDatabase(), is(value));
    }

    @Test
    public void shouldReturnDefaultValueWhenAbsent() {
      final LobbyPropertyReader lobbyPropertyReader = newLobbyPropertyReader(PropertyKeys.POSTGRES_DATABASE, "");

      assertThat(lobbyPropertyReader.getPostgresDatabase(), is(DefaultValues.POSTGRES_DATABASE));
    }
  }

  @Nested
  public final class GetPostgresHostTest {
    @Test
    public void shouldReturnValueFromDelegateWhenPresent() {
      final String value = "myhost.mydomain";
      final LobbyPropertyReader lobbyPropertyReader = newLobbyPropertyReader(PropertyKeys.POSTGRES_HOST, value);

      assertThat(lobbyPropertyReader.getPostgresHost(), is(value));
    }

    @Test
    public void shouldReturnDefaultValueWhenAbsent() {
      final LobbyPropertyReader lobbyPropertyReader = newLobbyPropertyReader(PropertyKeys.POSTGRES_HOST, "");

      assertThat(lobbyPropertyReader.getPostgresHost(), is(DefaultValues.POSTGRES_HOST));
    }
  }

  @Nested
  public final class GetPostgresPasswordTest {
    @Test
    public void shouldReturnValueFromDelegateWhenPresent() {
      final String value = "funnyPasssword";
      final LobbyPropertyReader lobbyPropertyReader = newLobbyPropertyReader(PropertyKeys.POSTGRES_PASSWORD, value);

      assertThat(lobbyPropertyReader.getPostgresPassword(), is(value));
    }

    @Test
    public void shouldReturnEmptyStringWhenAbsent() {
      final LobbyPropertyReader lobbyPropertyReader = newLobbyPropertyReader(PropertyKeys.POSTGRES_PASSWORD, "");

      assertThat(lobbyPropertyReader.getPostgresPassword(), is(emptyString()));
    }
  }

  @Nested
  public final class GetPostgresPortTest {
    @Test
    public void shouldReturnValueFromDelegateWhenPresent() {
      final int value = 1234;
      final LobbyPropertyReader lobbyPropertyReader =
          newLobbyPropertyReader(PropertyKeys.POSTGRES_PORT, String.valueOf(value));

      assertThat(lobbyPropertyReader.getPostgresPort(), is(value));
    }

    @Test
    public void shouldReturnDefaultValueWhenAbsent() {
      final LobbyPropertyReader lobbyPropertyReader = newLobbyPropertyReader(PropertyKeys.POSTGRES_PORT, "");

      assertThat(lobbyPropertyReader.getPostgresPort(), is(DefaultValues.POSTGRES_PORT));
    }

    @Test
    public void shouldThrowExceptionWhenMalformed() {
      final LobbyPropertyReader lobbyPropertyReader = newLobbyPropertyReader(PropertyKeys.POSTGRES_PORT, "other");

      assertThrows(NumberFormatException.class, () -> lobbyPropertyReader.getPostgresPort());
    }
  }

  @Nested
  public final class GetPostgresUserTest {
    @Test
    public void shouldReturnValueFromDelegateWhenPresent() {
      final String value = "funnyName";
      final LobbyPropertyReader lobbyPropertyReader = newLobbyPropertyReader(PropertyKeys.POSTGRES_USER, value);

      assertThat(lobbyPropertyReader.getPostgresUser(), is(value));
    }

    @Test
    public void shouldReturnEmptyStringWhenAbsent() {
      final LobbyPropertyReader lobbyPropertyReader = newLobbyPropertyReader(PropertyKeys.POSTGRES_USER, "");

      assertThat(lobbyPropertyReader.getPostgresUser(), is(emptyString()));
    }
  }

  @Nested
  public final class IsMaintenanceModeTest {
    @Test
    public void shouldReturnTrueWhenPresentAndEnabled() {
      Arrays.asList("true", "TRUE")
          .forEach(value -> {
            final LobbyPropertyReader lobbyPropertyReader =
                newLobbyPropertyReader(PropertyKeys.MAINTENANCE_MODE, value);
            assertThat(lobbyPropertyReader.isMaintenanceMode(), is(true));
          });
    }

    @Test
    public void shouldReturnFalseWhenPresentAndDisabled() {
      Arrays.asList("false", "FALSE", "other")
          .forEach(value -> {
            final LobbyPropertyReader lobbyPropertyReader =
                newLobbyPropertyReader(PropertyKeys.MAINTENANCE_MODE, value);
            assertThat(lobbyPropertyReader.isMaintenanceMode(), is(false));
          });
    }

    @Test
    public void shouldReturnFalseWhenAbsent() {
      final LobbyPropertyReader lobbyPropertyReader = newLobbyPropertyReader(PropertyKeys.MAINTENANCE_MODE, "");

      assertThat(lobbyPropertyReader.isMaintenanceMode(), is(false));
    }
  }
}
