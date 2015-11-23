package games.strategy.engine.framework.mapDownload;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;


/**
 * Does a basic test where a property file is written, then parsed back and we verify we get the expected value.
 */
public class MapDownloadPropertiesTest {

  private final static String SAMPLE_VALUE = "http://this is a test value.txt";

  private MapDownloadProperties testObj;

  @Before
  public void setUp() throws Exception {
    assertThat(MapDownloadProperties.MAP_LIST_DOWNLOAD_SITE_PROPERTY_KEY, notNullValue());
    String mapListProp = MapDownloadProperties.MAP_LIST_DOWNLOAD_SITE_PROPERTY_KEY + " = " + SAMPLE_VALUE;

    File testPropertiesFile = new File("mapDownload.test.properties");
    testPropertiesFile.deleteOnExit();
    Files.write(Paths.get(testPropertiesFile.getPath()), ImmutableList.of(mapListProp));

    testObj = new MapDownloadProperties(testPropertiesFile);
  }

  @Test
  public void mapListDownloadSitePropertyIsReadFromPropertyFile() {
    assertThat(testObj.getMapListDownloadSite(), is(SAMPLE_VALUE));
  }
}
