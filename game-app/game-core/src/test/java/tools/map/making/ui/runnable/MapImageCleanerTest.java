package tools.map.making.ui.runnable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

public class MapImageCleanerTest {
  @Test
  void testBasicFunctionality() {
    BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
    for (int x = 0; x < image.getWidth(); x++) {
      for (int y = 0; y < image.getHeight(); y++) {
        image.setRGB(x, y, Color.WHITE.getRGB());
      }
    }
    // Add a line splitting the image into two regions.
    for (int x = 0; x < image.getWidth(); x++) {
      image.setRGB(x, 4, Color.BLACK.getRGB());
    }
    // Set one of the above line's pixels to gray.
    image.setRGB(1, 4, Color.GRAY.getRGB());
    // Add an incomplete line, not splitting the image at all.
    for (int x = 0; x < image.getWidth() - 1; x++) {
      image.setRGB(x, 7, Color.BLACK.getRGB());
    }
    // Add a corner region of size 1.
    image.setRGB(0, 1, Color.BLACK.getRGB());
    image.setRGB(1, 0, Color.BLACK.getRGB());
    image.setRGB(1, 1, Color.BLACK.getRGB());

    // Clean the image with a min region size of 2.
    new MapImageCleaner(image, 2).cleanUpImage();
    // Check that the line splitting the image is still there.
    for (int x = 0; x < image.getWidth(); x++) {
      assertThat(image.getRGB(x, 4), is(Color.BLACK.getRGB()));
    }
    // Check that the incomplete line was removed.
    for (int x = 0; x < image.getWidth() - 1; x++) {
      assertThat(image.getRGB(x, 7), is(Color.WHITE.getRGB()));
    }
    // Check that the corner region has been removed, since it's below the min size.
    assertThat(image.getRGB(0, 1), is(Color.WHITE.getRGB()));
    assertThat(image.getRGB(1, 0), is(Color.WHITE.getRGB()));
    assertThat(image.getRGB(1, 1), is(Color.WHITE.getRGB()));
  }
}
