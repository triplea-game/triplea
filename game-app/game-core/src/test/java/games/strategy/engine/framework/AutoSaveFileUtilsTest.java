package games.strategy.engine.framework;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.settings.ClientSetting;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class AutoSaveFileUtilsTest extends AbstractClientSettingTestCase {
  private final AutoSaveFileUtils autoSaveFileUtils = new AutoSaveFileUtils();

  @Nested
  final class GetAutoSaveFileTest {
    @Test
    void shouldReturnFileInAutoSaveFolder() {
      ClientSetting.saveGamesFolderPath.setValue(Path.of("path", "to", "saves"));

      @NonNls final String fileName = "savegame.tsvg";
      assertThat(
          autoSaveFileUtils.getAutoSaveFile(fileName),
          is(Path.of("path", "to", "saves", "autoSave", fileName)));
    }
  }

  @Nested
  final class GetAutoSaveFileNameTest {
    private static final String BASE_FILE_NAME = "baseFileName";

    @Test
    void shouldNotPrefixFileNameWhenHeaded() {
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

  @Nested
  final class GetBeforeStepAutoSaveFileTest {
    @Test
    void shouldReturnFileNameWithCapitalizedStepName() {
      assertThat(
          autoSaveFileUtils.getBeforeStepAutoSaveFile("step").getFileName().toString(),
          is("autosaveBeforeStep.tsvg"));
    }
  }

  @Nested
  final class GetAfterStepAutoSaveFileTest {
    @Test
    void shouldReturnFileNameWithCapitalizedStepName() {
      assertThat(
          autoSaveFileUtils.getAfterStepAutoSaveFile("step").getFileName().toString(),
          is("autosaveAfterStep.tsvg"));
    }
  }
}
