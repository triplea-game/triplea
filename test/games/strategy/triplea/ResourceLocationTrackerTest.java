package games.strategy.triplea;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.net.URL;

import org.junit.Test;


public class ResourceLocationTrackerTest {
  private ResourceLocationTracker testObj;

  @Test
  public void defaultEmptyMapPrefix() {
    ResourceLocationTracker testObj = new ResourceLocationTracker("", new URL[0]);
    assertThat(testObj.getMapPrefix(), is(""));
  }

  @Test
  public void defaultEmptyMapPrefixWithMoreInterestingTestData() throws Exception {
    ResourceLocationTracker testObj =
        new ResourceLocationTracker("", new URL[] {new URL("file://localhost"), new URL("file://oldFormat.zip")});
    assertThat(testObj.getMapPrefix(), is(""));
  }

  @Test
  public void masterZipsGetSpecialPrefixBasedOnTheMapName() throws Exception {
    final String fakeMapName = "fakeMapName";
    ResourceLocationTracker testObj = new ResourceLocationTracker(fakeMapName,
        new URL[] {new URL("file://pretend" + ResourceLocationTracker.MASTER_ZIP_IDENTIFYING_SUFFIX)});
    assertThat(testObj.getMapPrefix(), is(fakeMapName + ResourceLocationTracker.MASTER_ZIP_MAGIC_PREFIX));
  }

}
