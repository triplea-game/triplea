package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import games.strategy.engine.framework.map.download.DownloadFile.DownloadState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DownloadFileTest {
  @Test
  void testBasicStartCancel() {
    final DownloadFileDescription downloadFileDescription =
        DownloadFileDescription.builder()
            .downloadUrl("url")
            .description("description")
            .mapName("mapName")
            .version(0)
            .mapCategory("BEST")
            .build();
    final DownloadFile testObj =
        new DownloadFile(downloadFileDescription, mock(DownloadListener.class));
    assertThat(testObj.getDownloadState(), is(DownloadState.NOT_STARTED));

    testObj.startAsyncDownload();
    assertThat(testObj.getDownloadState(), is(DownloadState.DOWNLOADING));

    testObj.cancelDownload();
    assertThat(testObj.getDownloadState(), is(DownloadState.CANCELLED));
  }

  @Test
  void normalizeMapName() {
    assertThat(DownloadFile.normalizeMapName("valid-name"), is("valid-name"));
    assertThat(DownloadFile.normalizeMapName("also_valid"), is("also_valid"));
    assertThat(
        "Ampersand is a valid map name but scary in a file system, should be stripped",
        DownloadFile.normalizeMapName("a&b"),
        is("ab"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"*", ".", "\"", "/", "\\", "[", "]", ":", ";", "|", ","})
  void invalidCharactersAreStripped(final String invalidCharacter) {
    assertThat(DownloadFile.normalizeMapName(invalidCharacter), is(""));
  }
}
