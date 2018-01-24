package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;

import org.junit.jupiter.api.Test;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.util.Version;

public class DownloadFileDescriptionTest extends AbstractClientSettingTestCase {

  @Test
  public void testIsMap() {
    final DownloadFileDescription testObj = new DownloadFileDescription("", "", "", new Version(0, 0),
        DownloadFileDescription.DownloadType.MAP, DownloadFileDescription.MapCategory.EXPERIMENTAL);
    assertThat(testObj.isMap(), is(true));
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
  public void testGetFeedbackUrl_ShouldReturnMapIssueTrackerUrlWhenDownloadUrlIsGitHubArchive() {
    assertThat(
        testObjFromUrl("https://github.com/org/repo/archive/master.zip").getFeedbackUrl(),
        is("https://github.com/org/repo/issues/new"));
  }

  @Test
  public void testGetFeedbackUrl_ShouldReturnGeneralIssueTrackerUrlWhenDownloadUrlIsNotGitHubArchive() {
    assertThat(
        "when URL does not contain 'github.com'",
        testObjFromUrl("https://somewhere-else.com/org/repo/archive/master.zip").getFeedbackUrl(),
        is(UrlConstants.GITHUB_ISSUES.toString()));
    assertThat(
        "when URL does not contain '/archive/'",
        testObjFromUrl("https://github.com/org/repo/releases/download/1.0/repo.zip").getFeedbackUrl(),
        is(UrlConstants.GITHUB_ISSUES.toString()));
  }

  @Test
  public void testGetInstallLocation() {
    String inputUrl = "http://github.com/abc.zip";
    String mapName = "123-map";
    File expected = new File(ClientFileSystemHelper.getUserMapsFolder() + File.separator + mapName + ".zip");

    mapInstallLocationTest(inputUrl, mapName, expected);

    inputUrl = "http://github.com/abc-master.zip";
    mapName = "best_map";
    expected = new File(ClientFileSystemHelper.getUserMapsFolder() + File.separator + mapName + "-master.zip");
    mapInstallLocationTest(inputUrl, mapName, expected);
  }

  private static void mapInstallLocationTest(final String inputUrl, final String mapName, final File expected) {
    final DownloadFileDescription testObj = new DownloadFileDescription(inputUrl, "", mapName, new Version(0, 0),
        DownloadFileDescription.DownloadType.MAP, DownloadFileDescription.MapCategory.EXPERIMENTAL);

    assertThat(testObj.getInstallLocation().getAbsolutePath(), is(expected.getAbsolutePath()));
  }
}
