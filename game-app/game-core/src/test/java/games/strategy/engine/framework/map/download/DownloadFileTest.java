package games.strategy.engine.framework.map.download;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import games.strategy.engine.framework.map.download.DownloadFile.DownloadState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;

class DownloadFileTest {
  @Test
  void testBasicStartCancel() {
    final MapDownloadItem mapDownloadItem =
        MapDownloadItem.builder()
            .downloadUrl("url")
            .previewImageUrl("preview-url")
            .description("description")
            .mapName("mapName")
            .lastCommitDateEpochMilli(60L)
            .downloadSizeInBytes(100L)
            .build();
    final DownloadFile testObj = new DownloadFile(mapDownloadItem, mock(DownloadListener.class));
    assertThat(testObj.getDownloadState()).isEqualTo(DownloadState.NOT_STARTED);

    testObj.startAsyncDownload();
    assertThat(testObj.getDownloadState()).isEqualTo(DownloadState.DOWNLOADING);

    testObj.cancelDownload();
    assertThat(testObj.getDownloadState()).isEqualTo(DownloadState.CANCELLED);
  }

  @Test
  void normalizeMapName() {
    assertThat(DownloadFile.normalizeMapName("valid-name")).isEqualTo("valid-name");
    assertThat(DownloadFile.normalizeMapName("also_valid")).isEqualTo("also_valid");
    assertThat(DownloadFile.normalizeMapName("a&b"))
        .as("Ampersand is a valid map name but scary in a file system, should be stripped")
        .isEqualTo("ab");
  }

  @ParameterizedTest
  @ValueSource(strings = {"*", ".", "\"", "/", "\\", "[", "]", ":", ";", "|", ","})
  void invalidCharactersAreStripped(final String invalidCharacter) {
    assertThat(DownloadFile.normalizeMapName(invalidCharacter)).isEqualTo("");
  }
}
