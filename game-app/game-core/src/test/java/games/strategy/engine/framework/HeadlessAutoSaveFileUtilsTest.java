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
    @Test
    void shouldPrefixFileName() {
      System.setProperty(CliProperties.TRIPLEA_NAME, "hostName");

      assertThat(
          autoSaveFileUtils.getAutoSaveFileName("baseFileName"),
          is("autosave_hostName_baseFileName"));
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
          is("autosave_connection_lost_on_May_09_at_22_08.tsvg"));
    }
  }
}
