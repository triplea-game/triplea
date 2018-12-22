package org.triplea.lobby.server.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.common.config.MemoryPropertyReader;
import org.triplea.lobby.server.config.LobbyConfiguration.DefaultValues;
import org.triplea.lobby.server.config.LobbyConfiguration.PropertyKeys;

public final class LobbyConfigurationTest {
  private final MemoryPropertyReader memoryPropertyReader = new MemoryPropertyReader();
  private final LobbyConfiguration lobbyConfiguration = new LobbyConfiguration(memoryPropertyReader);

  @Nested
  public final class GetPortTest {
    @Test
    public void shouldReturnValueWhenPresent() {
      final int value = 100;
      memoryPropertyReader.setProperty(PropertyKeys.PORT, String.valueOf(value));

      assertThat(lobbyConfiguration.getPort(), is(value));
    }

    @Test
    public void shouldReturnDefaultValueWhenAbsent() {
      memoryPropertyReader.setProperty(PropertyKeys.PORT, "");

      assertThat(lobbyConfiguration.getPort(), is(DefaultValues.PORT));
    }
  }

  @Nested
  public final class GetPostgresDatabaseTest {
    @Test
    public void shouldReturnValueWhenPresent() {
      final String value = "database";
      memoryPropertyReader.setProperty(PropertyKeys.POSTGRES_DATABASE, value);

      assertThat(lobbyConfiguration.getPostgresDatabase(), is(value));
    }

    @Test
    public void shouldReturnDefaultValueWhenAbsent() {
      memoryPropertyReader.setProperty(PropertyKeys.POSTGRES_DATABASE, "");

      assertThat(lobbyConfiguration.getPostgresDatabase(), is(DefaultValues.POSTGRES_DATABASE));
    }
  }

  @Nested
  public final class GetPostgresHostTest {
    @Test
    public void shouldReturnValueWhenPresent() {
      final String value = "myhost.mydomain";
      memoryPropertyReader.setProperty(PropertyKeys.POSTGRES_HOST, value);

      assertThat(lobbyConfiguration.getPostgresHost(), is(value));
    }

    @Test
    public void shouldReturnDefaultValueWhenAbsent() {
      memoryPropertyReader.setProperty(PropertyKeys.POSTGRES_HOST, "");

      assertThat(lobbyConfiguration.getPostgresHost(), is(DefaultValues.POSTGRES_HOST));
    }
  }

  @Nested
  public final class GetPostgresPasswordTest {
    @Test
    public void shouldReturnValueWhenPresent() {
      final String value = "funnyPasssword";
      memoryPropertyReader.setProperty(PropertyKeys.POSTGRES_PASSWORD, value);

      assertThat(lobbyConfiguration.getPostgresPassword(), is(value));
    }

    @Test
    public void shouldReturnEmptyStringWhenAbsent() {
      memoryPropertyReader.setProperty(PropertyKeys.POSTGRES_PASSWORD, "");

      assertThat(lobbyConfiguration.getPostgresPassword(), is(emptyString()));
    }
  }

  @Nested
  public final class GetPostgresPortTest {
    @Test
    public void shouldReturnValueWhenPresent() {
      final int value = 1234;
      memoryPropertyReader.setProperty(PropertyKeys.POSTGRES_PORT, String.valueOf(value));

      assertThat(lobbyConfiguration.getPostgresPort(), is(value));
    }

    @Test
    public void shouldReturnDefaultValueWhenAbsent() {
      memoryPropertyReader.setProperty(PropertyKeys.POSTGRES_PORT, "");

      assertThat(lobbyConfiguration.getPostgresPort(), is(DefaultValues.POSTGRES_PORT));
    }
  }

  @Nested
  public final class GetPostgresUserTest {
    @Test
    public void shouldReturnValueWhenPresent() {
      final String value = "funnyName";
      memoryPropertyReader.setProperty(PropertyKeys.POSTGRES_USER, value);

      assertThat(lobbyConfiguration.getPostgresUser(), is(value));
    }

    @Test
    public void shouldReturnEmptyStringWhenAbsent() {
      memoryPropertyReader.setProperty(PropertyKeys.POSTGRES_USER, "");

      assertThat(lobbyConfiguration.getPostgresUser(), is(emptyString()));
    }
  }
}
