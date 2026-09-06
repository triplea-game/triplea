package games.strategy.engine.framework;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.settings.ClientSetting;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@NonNls
final class AutoSaveFileUtilsTest extends AbstractClientSettingTestCase {
  private final AutoSaveFileUtils autoSaveFileUtils = new AutoSaveFileUtils();

  @Nested
  final class GetAutoSaveFileTest {
    @Test
    void shouldReturnFileInAutoSaveFolder() {
      ClientSetting.saveGamesFolderPath.setValue(Path.of("path", "to", "saves"));

      @NonNls final String fileName = "savegame.tsvg";
      assertThat(autoSaveFileUtils.getAutoSaveFile(fileName))
          .isEqualTo(Path.of("path", "to", "saves", "autoSave", fileName));
    }
  }

  @Nested
  final class GetAutoSaveFileNameTest {
    @NonNls private static final String BASE_FILE_NAME = "baseFileName";

    @Test
    void shouldNotPrefixFileNameWhenHeaded() {
      assertThat(autoSaveFileUtils.getAutoSaveFileName(BASE_FILE_NAME)).isEqualTo(BASE_FILE_NAME);
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
          .isEqualTo("connection_lost_on_May_09_at_22_08.tsvg");
    }
  }

  @Nested
  final class GetBeforeStepAutoSaveFileTest {
    @Test
    void shouldReturnFileNameWithCapitalizedStepName() {
      assertThat(autoSaveFileUtils.getBeforeStepAutoSaveFile("step").getFileName().toString())
          .isEqualTo("autosaveBeforeStep.tsvg");
    }
  }

  @Nested
  final class GetAfterStepAutoSaveFileTest {
    @Test
    void shouldReturnFileNameWithCapitalizedStepName() {
      assertThat(autoSaveFileUtils.getAfterStepAutoSaveFile("step").getFileName().toString())
          .isEqualTo("autosaveAfterStep.tsvg");
    }
  }
}
