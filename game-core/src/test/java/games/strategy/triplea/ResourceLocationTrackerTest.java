package games.strategy.triplea;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.net.URL;

import org.junit.jupiter.api.Test;


public class ResourceLocationTrackerTest {
  @Test
  public void defaultEmptyMapPrefix() {
    final ResourceLocationTracker testObj = new ResourceLocationTracker("", new URL[0]);
    assertThat(testObj.getMapPrefix(), is(""));
  }

  @Test
  public void defaultEmptyMapPrefixWithMoreInterestingTestData() throws Exception {
    final ResourceLocationTracker testObj =
        new ResourceLocationTracker("", new URL[] {new URL("file://localhost"), new URL("file://oldFormat.zip")});
    assertThat(testObj.getMapPrefix(), is(""));
  }

  @Test
  public void masterZipsGetSpecialPrefixBasedOnTheMapName() throws Exception {
    final String fakeMapName = "fakeMapName";
    final ResourceLocationTracker testObj = new ResourceLocationTracker(fakeMapName,
        new URL[] {new URL("file://pretend" + ResourceLocationTracker.MASTER_ZIP_IDENTIFYING_SUFFIX)});
    assertThat(testObj.getMapPrefix(), is(fakeMapName + ResourceLocationTracker.MASTER_ZIP_MAGIC_PREFIX));
  }

}
