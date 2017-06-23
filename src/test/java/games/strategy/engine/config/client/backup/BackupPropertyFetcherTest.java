package games.strategy.engine.config.client.backup;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

import games.strategy.engine.lobby.client.login.LobbyServerProperties;

public class BackupPropertyFetcherTest {
  @Test
  public void parseBackupValuesFromEngineConfig() {
    final BackupPropertyFetcher testObj = new BackupPropertyFetcher();

    final String host = "abc";
    final String port = "123";

    final LobbyServerProperties resultValue = testObj.parseBackupValuesFromEngineConfig(host + ":" + port);

    assertThat(resultValue.host, is(host));
    assertThat(resultValue.port, is(Integer.parseInt(port)));
    assertThat("an error message being set would indicate that the lobby is unavailable. We expect this to "
            + "be empty to allow a connection using backup config.",
        resultValue.serverErrorMessage, is(""));
    assertThat(resultValue.serverMessage, is(""));
  }

  @Test
  public void illegalValuesWillThrow() {
    Arrays.asList(
        null,
        "host_missing_port",
        "still_missing_port:",
        "not_numeric_port:abc",
        ":123",
        ":",
        "multiple_colons_not_allowed:123:invalid"
    ).forEach(invalidValue -> {
      try {
        new BackupPropertyFetcher().parseBackupValuesFromEngineConfig(invalidValue);
        fail("Expected value: " + invalidValue + ", to generate an input exception");
      } catch (final BackupPropertyFetcher.InvalidLobbyAddressException expected) {
        // expected
      }
    });
  }
}
