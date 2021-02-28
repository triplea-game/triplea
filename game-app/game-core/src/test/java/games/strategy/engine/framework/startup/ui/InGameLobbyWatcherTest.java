package games.strategy.engine.framework.startup.ui;

import static games.strategy.engine.framework.startup.ui.InGameLobbyWatcher.getLobbySystemProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class InGameLobbyWatcherTest {
  @Nested
  final class GetLobbySystemPropertyTest {
    private static final String KEY = "__GetLobbySystemPropertyTest__key";
    private static final String BACKUP_KEY = KEY + ".backup";
    private static final String VALUE = "primaryValue";
    private static final String BACKUP_VALUE = "backupValue";

    @AfterEach
    void clearSystemProperties() {
      givenPrimaryValueNotSet();
      givenBackupValueNotSet();
    }

    private void givenPrimaryValueSet() {
      System.setProperty(KEY, VALUE);
    }

    private void givenPrimaryValueNotSet() {
      System.clearProperty(KEY);
    }

    private void givenBackupValueSet() {
      System.setProperty(BACKUP_KEY, BACKUP_VALUE);
    }

    private void givenBackupValueNotSet() {
      System.clearProperty(BACKUP_KEY);
    }

    @Test
    void shouldReturnPrimaryValueWhenPrimaryValueSet() {
      givenPrimaryValueSet();

      assertThat(getLobbySystemProperty(KEY), is(VALUE));
    }

    @Test
    void shouldCopyPrimaryValueToBackupValueWhenPrimaryValueSet() {
      givenPrimaryValueSet();

      getLobbySystemProperty(KEY);

      assertThat(System.getProperty(BACKUP_KEY), is(VALUE));
    }

    @Test
    void shouldReturnBackupValueWhenPrimaryValueNotSet() {
      givenPrimaryValueNotSet();
      givenBackupValueSet();

      assertThat(getLobbySystemProperty(KEY), is(BACKUP_VALUE));
    }

    @Test
    void shouldReturnNullWhenPrimaryValueNotSetAndBackupValueNotSet() {
      givenPrimaryValueNotSet();
      givenBackupValueNotSet();

      assertThat(getLobbySystemProperty(KEY), is(nullValue()));
    }
  }
}
