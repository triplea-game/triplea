package games.strategy.triplea;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.net.URL;
import org.junit.jupiter.api.Test;

class ResourceLocationTrackerTest {
  @Test
  void defaultEmptyMapPrefix() {
    assertThat(ResourceLocationTracker.getMapPrefix("", new URL[0]), is(""));
  }

  @Test
  void defaultEmptyMapPrefixWithMoreInterestingTestData() throws Exception {
    assertThat(
        ResourceLocationTracker.getMapPrefix(
            "", new URL[] {new URL("file://localhost"), new URL("file://oldFormat.zip")}),
        is(""));
  }

  @Test
  void masterZipsGetSpecialPrefixBasedOnTheMapName() throws Exception {
    final String fakeMapName = "fakeMapName";
    assertThat(
        ResourceLocationTracker.getMapPrefix(
            fakeMapName,
            new URL[] {
              new URL("file://pretend" + ResourceLocationTracker.MASTER_ZIP_IDENTIFYING_SUFFIX)
            }),
        is(fakeMapName + ResourceLocationTracker.MASTER_ZIP_MAGIC_PREFIX));
  }
}
