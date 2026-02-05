package games.strategy.triplea;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Unit tests for the {@link ResourceLoader} class when running the game Headed. */
final class HeadedResourceLoaderTest {
  private final ResourceLoader resourceLoader = new ResourceLoader(Collections.emptyList());

  @Test
  void canLoadImage() {
    // TODO: Extract these image files and dirs to centralized constants
    var fileLocation = ResourceLoader.getAssetsFileLocation("launch_screens", "triplea-logo.png");
    var image = resourceLoader.loadBufferedImage(fileLocation);
    assertThat(image.isPresent());
  }

  @Test
  void createResourcePathString_failsForEmptyPathResult() {
    Assertions.assertThrows(
        AssertionError.class,
        () -> {
          resourceLoader.createResourcePathString("", new String[] {});
        });
  }

  @Test
  void createResourcePathString_failsForSlashPathResult() {
    Assertions.assertThrows(
        AssertionError.class,
        () -> {
          resourceLoader.createResourcePathString("", new String[] {""});
        });
  }

  /** This tests building various resource paths to images. It should succeed regardless of OS. */
  @ParameterizedTest(name = "{index}: first=\"{0}\" rest={1}")
  @MethodSource("getPossibleTestResources")
  void createResourcePathString_isPlatformIndependent(
      String first, String[] rest, String expected) {
    String actual = resourceLoader.createResourcePathString(first, rest);
    assertThat(expected).isEqualTo(actual);
  }

  private static Stream<Arguments> getPossibleTestResources() {
    return Stream.of(
        // Simple, normal path
        Arguments.of(
            "assets",
            new String[] {"territoryNames", "Hebei", "Peking.png"},
            "assets/territoryNames/Hebei/Peking.png"),

        // Slash inside filename (LEGAL resource, ILLEGAL Windows Path)
        Arguments.of(
            "assets",
            new String[] {"territoryNames", "Hebei / Peking.png"},
            "assets/territoryNames/Hebei / Peking.png"),

        // Trailing space in directory name (breaks Windows Path)
        Arguments.of(
            "assets",
            new String[] {"territoryNames", "Hebei ", "Peking.png"},
            "assets/territoryNames/Hebei /Peking.png"),

        // Windows-forbidden characters
        Arguments.of(
            "assets",
            new String[] {"territoryNames", "A:B*C?D", "img.png"},
            "assets/territoryNames/A:B*C?D/img.png"),

        // Unicode + slash
        Arguments.of(
            "assets",
            new String[] {"territoryNames", "河北 / 北京.png"},
            "assets/territoryNames/河北 / 北京.png"),

        // Single element only
        Arguments.of("Peking.png", new String[] {}, "Peking.png"),

        // Empty first element
        Arguments.of("", new String[] {"assets", "Peking.png"}, "/assets/Peking.png"));
  }
}
