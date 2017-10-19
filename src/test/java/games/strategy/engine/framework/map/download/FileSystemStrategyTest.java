package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.util.Optional;

import org.junit.experimental.extensions.TemporaryFolder;
import org.junit.experimental.extensions.TemporaryFolderExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.common.io.Files;

import games.strategy.util.Version;

/**
 * For transition reasons we use a DownloadFileProperties to read
 * a properties file for each map that we download. Reading XMLs in Zips is can be
 * fast, so one day we should just read the versions directly from the map zip files.
 */
@ExtendWith(TemporaryFolderExtension.class)
public class FileSystemStrategyTest {

  private TemporaryFolder temporaryFolder;

  private FileSystemAccessStrategy testObj;

  private File mapFile;

  @BeforeEach
  public void setUp() throws Exception {
    testObj = new FileSystemAccessStrategy();
    final String text = DownloadFileProperties.VERSION_PROPERTY + " = 1.2";
    mapFile = temporaryFolder.newFile(getClass().getName());
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
