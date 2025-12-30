package games.strategy.triplea;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link EngineImageLoader} class when running the game Headed. */
final class HeadedEngineImageLoaderTest {
  @Test
  void canFindAssets() {
    var image = EngineImageLoader.loadImage("images", "up.gif"); // Arbitrary asset file
    Assertions.assertNotNull(image);
  }
}
