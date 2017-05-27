package games.strategy.engine.framework.map.download;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.io.Files;

import games.strategy.util.Version;

/**
 * For transition reasons we use a DownloadFileProperties to read
 * a properties file for each map that we download. Reading XMLs in Zips is can be
 * fast, so one day we should just read the versions directly from the map zip files.
 */
public class FileSystemStrategyTest {
  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private FileSystemAccessStrategy testObj;

  private File mapFile;

  @Before
  public void setUp() throws Exception {
    testObj = new FileSystemAccessStrategy();
    final String text = DownloadFileProperties.VERSION_PROPERTY + " = 1.2";
    mapFile = temporaryFolder.newFile();
    final File propFile = temporaryFolder.newFile(mapFile.getName() + ".properties");
    Files.write(text.getBytes(), propFile);
  }

  @Test
  public void testMapPropertyFileNotFound() {
    assertThat(testObj.getMapVersion("does_not_exist"), is(Optional.empty()));
  }

  @Test
  public void testMapFileFound() {
    assertThat(testObj.getMapVersion(mapFile.getAbsolutePath()), is(Optional.of(new Version(1, 2))));
  }

}
