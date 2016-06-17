package games.strategy.engine.framework.map.download;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Files;

import games.strategy.test.TestUtil;
import games.strategy.util.Version;

/**
 * For transition reasons we use a DownloadFileProperties to read
 * a properties file for each map that we download. Reading XMLs in Zips is can be
 * fast, so one day we should just read the versions directly from the map zip files.
 */
public class FileSystemStrategyTest {

  private FileSystemAccessStrategy testObj;

  private File mapFile;

  @Before
  public void setUp() throws Exception {
    testObj = new FileSystemAccessStrategy();
    String text = DownloadFileProperties.VERSION_PROPERTY + " = 1.2";
    mapFile = TestUtil.createTempFile("");
    File propFile = new File(mapFile.getAbsolutePath() + ".properties");
    Files.write(text.getBytes(),  propFile);
  }

  @Test
  public void testMapPropertyFileNotFound() {
    assertEquals(Optional.empty(), testObj.getMapVersion("does_not_exist"));
  }

  @Test
  public void testMapFileFound()  {
    assertEquals(Optional.of(new Version(1,2)), testObj.getMapVersion(mapFile.getAbsolutePath()));
  }

}
