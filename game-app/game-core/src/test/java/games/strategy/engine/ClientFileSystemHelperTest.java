package games.strategy.engine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.maps.listing.MapsClient;

@SuppressWarnings("InnerClassMayBeStatic")
final class ClientFileSystemHelperTest {
  @Nested
  final class GetUserMapsFolderTest extends AbstractClientSettingTestCase {
    @Test
    void shouldReturnCurrentFolderWhenOverrideFolderNotSet() {
      final Path result =
          ClientFileSystemHelper.getUserMapsFolder(() -> Path.of("/path/to/current"));

      assertThat(result, is(Path.of("/path", "to", "current", ClientFileSystemHelper.MAPS_FOLDER_NAME)));
    }
  }
}
