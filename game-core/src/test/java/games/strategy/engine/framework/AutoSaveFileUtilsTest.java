package games.strategy.engine.framework;

import static games.strategy.engine.framework.AutoSaveFileUtils.getAfterStepAutoSaveFile;
import static games.strategy.engine.framework.AutoSaveFileUtils.getAutoSaveFile;
import static games.strategy.engine.framework.AutoSaveFileUtils.getAutoSaveFileName;
import static games.strategy.engine.framework.AutoSaveFileUtils.getBeforeStepAutoSaveFile;
import static games.strategy.engine.framework.AutoSaveFileUtils.getLostConnectionAutoSaveFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.nio.file.Paths;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.settings.ClientSetting;

final class AutoSaveFileUtilsTest extends AbstractClientSettingTestCase {
  @Nested
  final class GetAutoSaveFileTest {
    @Test
    void shouldReturnFileInAutoSaveFolder() {
      ClientSetting.saveGamesFolderPath.setValue(Paths.get("path", "to", "saves"));

      final String fileName = "savegame.tsvg";
      assertThat(
          getAutoSaveFile(fileName),
          is(Paths.get("path", "to", "saves", "autoSave", fileName).toFile()));
    }
  }

  @Nested
  final class GetAutoSaveFileNameTest {
    private static final String BASE_FILE_NAME = "baseFileName";
    private static final String HOST_NAME = "hostName";
    private static final String PLAYER_NAME = "playerName";

    private void givenHostName(final String hostName) {
      System.setProperty(CliProperties.LOBBY_GAME_HOSTED_BY, hostName);
    }

    private void givenHostNameNotDefined() {
      System.clearProperty(CliProperties.LOBBY_GAME_HOSTED_BY);
    }

    private void givenPlayerName(final String playerName) {
      System.setProperty(CliProperties.TRIPLEA_NAME, playerName);
    }

    private void givenPlayerNameNotDefined() {
      System.clearProperty(CliProperties.TRIPLEA_NAME);
    }

    @AfterEach
    void clearSystemProperties() {
      givenHostNameNotDefined();
      givenPlayerNameNotDefined();
    }

    @Test
    void shouldNotPrefixFileNameWhenHeaded() {
      assertThat(getAutoSaveFileName(BASE_FILE_NAME, false), is(BASE_FILE_NAME));
    }

    @Test
    void shouldPrefixFileNameWithPlayerNameWhenHeadless() {
      givenPlayerName(PLAYER_NAME);
      givenHostName(HOST_NAME);

      assertThat(getAutoSaveFileName(BASE_FILE_NAME, true), is(PLAYER_NAME + "_" + BASE_FILE_NAME));
    }

    @Test
    void shouldPrefixFileNameWithHostNameWhenHeadlessAndPlayerNameNotDefined() {
      givenPlayerNameNotDefined();
      givenHostName(HOST_NAME);

      assertThat(getAutoSaveFileName(BASE_FILE_NAME, true), is(HOST_NAME + "_" + BASE_FILE_NAME));
    }

    @Test
    void shouldNotPrefixFileNameWhenHeadlessAndPlayerNameNotDefinedAndHostNameNotDefined() {
      givenPlayerNameNotDefined();
      givenHostNameNotDefined();

      assertThat(getAutoSaveFileName(BASE_FILE_NAME, true), is(BASE_FILE_NAME));
    }
  }

  @Nested
  final class GetLostConnectionAutoSaveFileTest {
    @Test
    void shouldReturnFileNameWithLocalDateTime() {
      assertThat(
          getLostConnectionAutoSaveFile(LocalDateTime.of(2008, 5, 9, 22, 8)).getName(),
          is("connection_lost_on_May_09_at_22_08.tsvg"));
    }
  }

  @Nested
  final class GetBeforeStepAutoSaveFileTest {
    @Test
    void shouldReturnFileNameWithCapitalizedStepName() {
      assertThat(getBeforeStepAutoSaveFile("step", false).getName(), is("autosaveBeforeStep.tsvg"));
    }
  }

  @Nested
  final class GetAfterStepAutoSaveFileTest {
    @Test
    void shouldReturnFileNameWithCapitalizedStepName() {
      assertThat(getAfterStepAutoSaveFile("step", false).getName(), is("autosaveAfterStep.tsvg"));
    }
  }
}
