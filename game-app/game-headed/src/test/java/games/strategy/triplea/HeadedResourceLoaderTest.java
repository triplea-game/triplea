package games.strategy.triplea;

import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link ResourceLoader} class when running the game Headed. */
final class HeadedResourceLoaderTest {
  @Test
  void canLoadImage() {
    // TODO: Extract these image files and dirs to centralized constants
    var image = ResourceLoader.loadImageAsset(Path.of("launch_screens", "triplea-logo.png"));
    Assertions.assertNotNull(image);
  }
}
