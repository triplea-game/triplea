package games.strategy.engine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;

import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("InnerClassMayBeStatic")
final class ClientFileSystemHelperTest {

  @Test
  void getRootFolder() {
    Path rootFolder = ClientFileSystemHelper.getRootFolder();

    assertThat(rootFolder, is(notNullValue()));
    assertThat("should be a folder", Files.isDirectory(rootFolder));
    assertThat(
        "should contain the '.triplea-root' touch file",
        Files.exists(rootFolder.resolve(".triplea-root")));
  }

  @Nested
  final class GetUserMapsFolderTest extends AbstractClientSettingTestCase {
    @Test
    void shouldReturnCurrentFolderWhenOverrideFolderNotSet() {
      final Path result =
          ClientFileSystemHelper.getUserMapsFolder(() -> Path.of("/path/to/current"));

      assertThat(result, is(Path.of("/path", "to", "current", "downloadedMaps")));
    }
  }
}
