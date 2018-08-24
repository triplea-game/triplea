package games.strategy.engine.lobby.server.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import games.strategy.engine.config.MemoryPropertyReader;
import games.strategy.engine.lobby.server.config.LobbyPropertyReader.DefaultValues;
import games.strategy.engine.lobby.server.config.LobbyPropertyReader.PropertyKeys;

public final class LobbyPropertyReaderTest {
  private final MemoryPropertyReader memoryPropertyReader = new MemoryPropertyReader();
  private final LobbyPropertyReader lobbyPropertyReader = new LobbyPropertyReader(memoryPropertyReader);

  @Nested
  public final class GetPortTest {
    @Test
    public void shouldReturnValueWhenPresent() {
      final int value = 100;
      memoryPropertyReader.setProperty(PropertyKeys.PORT, String.valueOf(value));

      assertThat(lobbyPropertyReader.getPort(), is(value));
    }

    @Test
    public void shouldReturnDefaultValueWhenAbsent() {
      memoryPropertyReader.setProperty(PropertyKeys.PORT, "");

      assertThat(lobbyPropertyReader.getPort(), is(DefaultValues.PORT));
    }
  }

  @Nested
  public final class GetPostgresDatabaseTest {
    @Test
    public void shouldReturnValueWhenPresent() {
      final String value = "database";
      memoryPropertyReader.setProperty(PropertyKeys.POSTGRES_DATABASE, value);

      assertThat(lobbyPropertyReader.getPostgresDatabase(), is(value));
    }

    @Test
    public void shouldReturnDefaultValueWhenAbsent() {
      memoryPropertyReader.setProperty(PropertyKeys.POSTGRES_DATABASE, "");

      assertThat(lobbyPropertyReader.getPostgresDatabase(), is(DefaultValues.POSTGRES_DATABASE));
    }
  }

  @Nested
  public final class GetPostgresHostTest {
    @Test
    public void shouldReturnValueWhenPresent() {
      final String value = "myhost.mydomain";
      memoryPropertyReader.setProperty(PropertyKeys.POSTGRES_HOST, value);

      assertThat(lobbyPropertyReader.getPostgresHost(), is(value));
    }

    @Test
    public void shouldReturnDefaultValueWhenAbsent() {
      memoryPropertyReader.setProperty(PropertyKeys.POSTGRES_HOST, "");

      assertThat(lobbyPropertyReader.getPostgresHost(), is(DefaultValues.POSTGRES_HOST));
    }
  }

  @Nested
  public final class GetPostgresPasswordTest {
    @Test
    public void shouldReturnValueWhenPresent() {
      final String value = "funnyPasssword";
      memoryPropertyReader.setProperty(PropertyKeys.POSTGRES_PASSWORD, value);

      assertThat(lobbyPropertyReader.getPostgresPassword(), is(value));
    }

    @Test
    public void shouldReturnEmptyStringWhenAbsent() {
      memoryPropertyReader.setProperty(PropertyKeys.POSTGRES_PASSWORD, "");

      assertThat(lobbyPropertyReader.getPostgresPassword(), is(emptyString()));
    }
  }

  @Nested
  public final class GetPostgresPortTest {
    @Test
    public void shouldReturnValueWhenPresent() {
      final int value = 1234;
      memoryPropertyReader.setProperty(PropertyKeys.POSTGRES_PORT, String.valueOf(value));

      assertThat(lobbyPropertyReader.getPostgresPort(), is(value));
    }

    @Test
    public void shouldReturnDefaultValueWhenAbsent() {
      memoryPropertyReader.setProperty(PropertyKeys.POSTGRES_PORT, "");

      assertThat(lobbyPropertyReader.getPostgresPort(), is(DefaultValues.POSTGRES_PORT));
    }
  }

  @Nested
  public final class GetPostgresUserTest {
    @Test
    public void shouldReturnValueWhenPresent() {
      final String value = "funnyName";
      memoryPropertyReader.setProperty(PropertyKeys.POSTGRES_USER, value);

      assertThat(lobbyPropertyReader.getPostgresUser(), is(value));
    }

    @Test
    public void shouldReturnEmptyStringWhenAbsent() {
      memoryPropertyReader.setProperty(PropertyKeys.POSTGRES_USER, "");

      assertThat(lobbyPropertyReader.getPostgresUser(), is(emptyString()));
    }
  }

  @Nested
  public final class IsMaintenanceModeTest {
    @Test
    public void shouldReturnValueWhenPresent() {
      final boolean value = !DefaultValues.MAINTENANCE_MODE;
      memoryPropertyReader.setProperty(PropertyKeys.MAINTENANCE_MODE, String.valueOf(value));

      assertThat(lobbyPropertyReader.isMaintenanceMode(), is(value));
    }

    @Test
    public void shouldReturnDefaultValueWhenAbsent() {
      memoryPropertyReader.setProperty(PropertyKeys.MAINTENANCE_MODE, "");

      assertThat(lobbyPropertyReader.isMaintenanceMode(), is(DefaultValues.MAINTENANCE_MODE));
    }
  }
}
