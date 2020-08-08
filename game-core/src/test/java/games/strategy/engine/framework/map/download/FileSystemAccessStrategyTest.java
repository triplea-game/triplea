package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.triplea.util.Version;

/**
 * For transition reasons we use a DownloadFileProperties to read a properties file for each map
 * that we download. Reading XMLs in Zips is can be fast, so one day we should just read the
 * versions directly from the map zip files.
 */
class FileSystemAccessStrategyTest {
  private FileSystemAccessStrategy testObj;
  private File mapFile;

  @BeforeEach
  void setUp(@TempDir final Path tempDirPath) throws Exception {
    testObj = new FileSystemAccessStrategy();
    final String text = DownloadFileProperties.VERSION_PROPERTY + " = 1.2";
    final Path mapPath = Files.createTempFile(tempDirPath, null, null);
    mapFile = mapPath.toFile();
    final Path mapPropsPath =
        Files.createFile(mapPath.resolveSibling(mapPath.getFileName() + ".properties"));
    Files.write(mapPropsPath, text.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void testMapPropertyFileNotFound() {
    assertThat(testObj.getMapVersion("does_not_exist"), is(Optional.empty()));
  }

  @Test
  void testMapFileFound() {
    assertThat(
        testObj.getMapVersion(mapFile.getAbsolutePath()), is(Optional.of(new Version(1, 2, 0))));
  }
}
