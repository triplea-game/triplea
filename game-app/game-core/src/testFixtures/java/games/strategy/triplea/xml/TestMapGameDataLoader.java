package games.strategy.triplea.xml;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TestAttachment;
import games.strategy.engine.data.gameparser.GameParser;
import games.strategy.engine.data.gameparser.XmlGameElementMapper;
import games.strategy.triplea.delegate.TestDelegate;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import org.triplea.util.Version;

/**
 * Utility class containing methods for loading XML map data from disk.
 *
 * <p>This class is meant to load files from a resource on the classpath. The files may exist on the
 * classpath, on in jars on the classpath. If a file is packaged in a jar, it will be extracted to a
 * temporary directory first.
 */
public final class TestMapGameDataLoader {
  private TestMapGameDataLoader() {
    throw new IllegalStateException("Utility class is not instantiable");
  }

  /**
   * Gets the game data for a map.
   *
   * @param fileName the filename of the map to load
   * @return The game data for the associated map.
   * @throws RuntimeException If an error occurs while loading the map.
   */
  public static GameData loadGameData(String fileName) {
    final Path dataPath;
    try {
      URI dataURL = TestMapGameDataLoader.class.getClassLoader().getResource(fileName).toURI();
      if (dataURL.getScheme().equals("jar")) {
        dataPath = extractJarResourceToTemp(dataURL.toURL());
      } else {
        dataPath = Path.of(dataURL);
      }
    } catch (final URISyntaxException | IOException e) {
      throw new IllegalStateException("Error finding or extracting file: " + fileName, e);
    }

    return GameParser.parse(
            dataPath,
            new XmlGameElementMapper(
                Map.of("TestDelegate", TestDelegate::new),
                Map.of("TestAttachment", TestAttachment::new)),
            new Version("2.0.0"),
            false)
        .orElseThrow(() -> new IllegalStateException("Error parsing: " + dataPath));
  }

  private static Path extractJarResourceToTemp(URL jarUrl) throws IOException {
    Preconditions.checkArgument(
        "jar".equals(jarUrl.getProtocol()), "URL must use \"jar:\" protocol. URL: " + jarUrl);

    JarURLConnection connection = (JarURLConnection) jarUrl.openConnection();
    String entryName = connection.getEntryName();

    Preconditions.checkArgument(
        entryName != null && !entryName.endsWith("/"),
        "URL does not point to a file. URL: " + jarUrl);

    Path tempDir = Files.createTempDirectory("jar-extract-");
    tempDir.toFile().deleteOnExit();

    Path outputFile = tempDir.resolve(Path.of(entryName).getFileName());
    try (InputStream in = connection.getInputStream()) {
      Files.copy(in, outputFile, StandardCopyOption.REPLACE_EXISTING);
    }
    return outputFile;
  }
}
