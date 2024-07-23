package games.strategy.engine.framework;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class HeadlessAutoSaveFileUtilsTest extends AbstractClientSettingTestCase {
  private final HeadlessAutoSaveFileUtils autoSaveFileUtils = new HeadlessAutoSaveFileUtils();

  @Nested
  final class GetAutoSaveFileNameTest {
    private static final String BASE_FILE_NAME = "baseFileName";
    private static final String HOST_NAME = "hostName";
    private static final String PLAYER_NAME = "playerName";

    private void givenHostName(final String hostName) {
      System.setProperty(CliProperties.TRIPLEA_NAME, hostName);
    }

    private void givenPlayerName(final String playerName) {
      System.setProperty(CliProperties.TRIPLEA_NAME, playerName);
    }

    @Test
    void shouldPrefixFileNameWithPlayerNameWhenHeadless() {
      givenPlayerName(PLAYER_NAME);

      assertThat(
          autoSaveFileUtils.getAutoSaveFileName(BASE_FILE_NAME),
          is(PLAYER_NAME + "_" + BASE_FILE_NAME));
    }

    @Test
    void shouldPrefixFileNameWithHostNameWhenHeadlessAndPlayerNameNotDefined() {
      givenHostName(HOST_NAME);

      assertThat(
          autoSaveFileUtils.getAutoSaveFileName(BASE_FILE_NAME),
          is(HOST_NAME + "_" + BASE_FILE_NAME));
    }

    @Test
    void shouldNotPrefixFileNameWhenHeadlessAndPlayerNameNotDefinedAndHostNameNotDefined() {
      assertThat(autoSaveFileUtils.getAutoSaveFileName(BASE_FILE_NAME), is(BASE_FILE_NAME));
    }
  }

  @Nested
  final class GetLostConnectionAutoSaveFileTest {
    @Test
    void shouldReturnFileNameWithLocalDateTime() {
      assertThat(
          autoSaveFileUtils
              .getLostConnectionAutoSaveFile(LocalDateTime.of(2008, 5, 9, 22, 8))
              .getFileName()
              .toString(),
          is("connection_lost_on_May_09_at_22_08.tsvg"));
    }
  }
}
