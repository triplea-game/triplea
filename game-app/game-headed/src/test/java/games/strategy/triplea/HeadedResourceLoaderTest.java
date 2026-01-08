package games.strategy.triplea;

import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link ResourceLoader} class when running the game Headed. */
final class HeadedResourceLoaderTest {
  private final ResourceLoader resourceLoader = new ResourceLoader(Collections.emptyList());

  @Test
  void canLoadImage() {
    // TODO: Extract these image files and dirs to centralized constants
    var fileLocation = ResourceLoader.getAssetsFileLocation("launch_screens", "triplea-logo.png");
    var image = resourceLoader.loadBufferedImage(fileLocation);
    Assertions.assertTrue(image.isPresent());
  }
}
