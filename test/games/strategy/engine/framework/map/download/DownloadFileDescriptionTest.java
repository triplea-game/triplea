package games.strategy.engine.framework.map.download;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.Test;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.util.Version;

public class DownloadFileDescriptionTest {

  @Test
  public void testIsMap() {
    final DownloadFileDescription testObj = new DownloadFileDescription("", "", "", new Version(0, 0),
        DownloadFileDescription.DownloadType.MAP, DownloadFileDescription.MapCategory.EXPERIMENTAL);
    assertThat(testObj.isMap(), is(true));
  }

  @Test
  public void testIsMod() {
    final DownloadFileDescription testObj = new DownloadFileDescription("", "", "", new Version(0, 0),
        DownloadFileDescription.DownloadType.MAP_MOD, DownloadFileDescription.MapCategory.EXPERIMENTAL);
    assertThat(testObj.isMapMod(), is(true));
  }

  @Test
  public void testIsSkin() {
    final DownloadFileDescription testObj = new DownloadFileDescription("", "", "", new Version(0, 0),
        DownloadFileDescription.DownloadType.MAP_SKIN, DownloadFileDescription.MapCategory.EXPERIMENTAL);
    assertThat(testObj.isMapSkin(), is(true));
  }

  @Test
  public void testIsTool() {
    final DownloadFileDescription testObj = new DownloadFileDescription("", "", "", new Version(0, 0),
        DownloadFileDescription.DownloadType.MAP_TOOL, DownloadFileDescription.MapCategory.EXPERIMENTAL);
    assertThat(testObj.isMapTool(), is(true));

  }

  @Test
  public void testGetMapName() {
    final String mapName = "abc";
    final DownloadFileDescription testObj =
        new DownloadFileDescription("", "", mapName, new Version(0, 0), DownloadFileDescription.DownloadType.MAP,
            DownloadFileDescription.MapCategory.EXPERIMENTAL);
    assertThat(testObj.getMapName(), is(mapName));
  }

  @Test
  public void testGetMapType() {
    final DownloadFileDescription testObj =
        new DownloadFileDescription("", "", "", new Version(0, 0), DownloadFileDescription.DownloadType.MAP,
            DownloadFileDescription.MapCategory.BEST);
    assertThat(testObj.getMapCategory(), is(DownloadFileDescription.MapCategory.BEST));
  }


  @Test
  public void testGetMapFileName() {
    final String expectedFileName = "world_war_ii_revised.zip";
    String inputUrl = "https://github.com/triplea-maps/world_war_ii_revised/releases/download/0.1/" + expectedFileName;

    DownloadFileDescription testObj = testObjFromUrl(inputUrl);
    assertThat(testObj.getMapZipFileName(), is(expectedFileName));

    inputUrl = "http://abc.com/" + expectedFileName;
    testObj = testObjFromUrl(inputUrl);
    assertThat(testObj.getMapZipFileName(), is(expectedFileName));

    inputUrl = "abc.zip";
    testObj = testObjFromUrl(inputUrl);
    assertThat("Unable to parse a url, no last '/' character, return empty.", testObj.getMapZipFileName(), is(""));

  }

  private static DownloadFileDescription testObjFromUrl(final String inputUrl) {
    return new DownloadFileDescription(inputUrl, "", "", new Version(0, 0),
        DownloadFileDescription.DownloadType.MAP, DownloadFileDescription.MapCategory.EXPERIMENTAL);
  }

  @Test
  public void testGetFeedbackUrl() {
    final String commonPrefix = "http://github.com/triplea-maps/world_war_ii_revised/";
    String inputUrl = commonPrefix + "releases/download/0.1/abc.zip";
    String expected = commonPrefix + "issues/new";

    DownloadFileDescription testObj = testObjFromUrl(inputUrl);
    assertThat(testObj.getFeedbackUrl(), is(expected));


    inputUrl = "http://randomWebsite/releases/abc.zip";
    expected = "";

    testObj = testObjFromUrl(inputUrl);
    assertThat("Missing 'github.com' in the URL should return empty", testObj.getFeedbackUrl(), is(expected));


    inputUrl = "http://github.com/random/abc.zip";
    expected = "";

    testObj = testObjFromUrl(inputUrl);
    assertThat("Missing 'releases' in the URL, should return empty", testObj.getFeedbackUrl(), is(expected));
  }


  @Test
  public void testGetInstallLocation() {
    final String inputUrl = "http://github.com/abc.zip";
    final File f = new File(ClientFileSystemHelper.getUserMapsFolder() + File.separator + "abc.zip");

    final DownloadFileDescription testObj = testObjFromUrl(inputUrl);
    assertThat(testObj.getInstallLocation().getAbsolutePath(), is(f.getAbsolutePath()));
  }
}
