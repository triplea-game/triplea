package games.strategy.engine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.io.File;
import java.nio.file.Paths;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("InnerClassMayBeStatic")
final class ClientFileSystemHelperTest {
  @Nested
  final class GetUserMapsFolderTest extends AbstractClientSettingTestCase {
    @Test
    void shouldReturnCurrentFolderWhenOverrideFolderNotSet() {
      final File result =
          ClientFileSystemHelper.getUserMapsFolder(() -> new File("/path/to/current"));

      assertThat(result, is(Paths.get("/path", "to", "current", "downloadedMaps").toFile()));
    }
  }
}
