package games.strategy.engine.framework.mapDownload;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Files;

import games.strategy.util.Version;

/**
 * For transition reasons we use a DownloadFileProperties to read
 * a properties file for each map that we download. Reading XMLs in Zips is can be
 * fast, so one day we should just read the versions directly from the map zip files.
 */
public class FileSystemStrategyTest {


  private final static String mapFileName = "mapName";
  private final static String zipName = FileSystemStrategy.convertToFileName(mapFileName);

  private FileSystemStrategy testObj;

  @Before
  public void setUp() throws Exception {
    final File rootFolder = Files.createTempDir();
    rootFolder.deleteOnExit();
    testObj = new FileSystemStrategy(rootFolder);

    // if the map file does not exist, then we will not try to load the property file
    File mapFile = new File(rootFolder, zipName);
    Files.write("",  mapFile, java.nio.charset.StandardCharsets.UTF_8);
    File mapPropertyFile = new File(rootFolder, DownloadFileProperties.getPropertiesFileName(zipName));
    String text = DownloadFileProperties.VERSION_PROPERTY + " = 1.2";
    Files.write(text, mapPropertyFile, java.nio.charset.StandardCharsets.UTF_8);
  }

  @Test
  public void testMapPropertyFileNotFound() {
    assertThat(testObj.getMapVersion("does_not_exist"), is(Optional.empty()));
  }

  @Test
  public void testMapFileFound()  {
    assertThat(testObj.getMapVersion(mapFileName), is(Optional.of(new Version(1,2))));
  }

  @Test
  public void testConvertToMapName() {
    String testValue = "test_map";
    String expected = testValue + ".zip";
    assertThat(FileSystemStrategy.convertToFileName(testValue), is(expected));
  }
}
