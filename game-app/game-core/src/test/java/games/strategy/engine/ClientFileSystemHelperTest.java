package games.strategy.engine;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("InnerClassMayBeStatic")
final class ClientFileSystemHelperTest {
  @Nested
  final class GetUserMapsFolderTest extends AbstractClientSettingTestCase {
    @Test
    void shouldReturnCurrentFolderWhenOverrideFolderNotSet() {
      final Path result =
          ClientFileSystemHelper.getUserMapsFolder(() -> Path.of("/path/to/current"));

      assertThat(result)
          .isEqualTo(Path.of("/path", "to", "current", ClientFileSystemHelper.MAPS_FOLDER_NAME));
    }
  }
}
