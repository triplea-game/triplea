package games.strategy.engine.framework;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.time.LocalDateTime;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class HeadlessAutoSaveFileUtilsTest extends AbstractClientSettingTestCase {
  private final HeadlessAutoSaveFileUtils autoSaveFileUtils = new HeadlessAutoSaveFileUtils();

  @Nested
  final class GetAutoSaveFileNameTest {
    @NonNls private static final String SYSTEM_PROPERTY_TRIPLEA_NAME = CliProperties.TRIPLEA_NAME;

    @Test
    void shouldPrefixFileName() {
      System.setProperty(SYSTEM_PROPERTY_TRIPLEA_NAME, "hostName");

      assertThat(autoSaveFileUtils.getAutoSaveFileName("baseFileName"))
          .isEqualTo("autosave_hostName_baseFileName");
    }

    @AfterEach
    void initializeSystemProperty() {
      System.clearProperty(SYSTEM_PROPERTY_TRIPLEA_NAME);
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
                  .toString())
          .isEqualTo("autosave_connection_lost_on_May_09_at_22_08.tsvg");
    }
  }
}
