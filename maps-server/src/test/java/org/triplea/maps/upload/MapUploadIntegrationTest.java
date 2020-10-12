package org.triplea.maps.upload;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.maps.listing.MapDownloadListing;
import org.triplea.http.client.maps.listing.MapsListingClient;
import org.triplea.http.client.maps.upload.MapUploadClient;
import org.triplea.http.client.maps.upload.MapUploadResult;
import org.triplea.maps.server.http.MapServerTest;
import org.triplea.test.common.Integration;

/**
 * In this test, zip up a sample map directory, upload it, then verify the file is available in map
 * download listing.
 *
 * <p>Requires:
 *
 * <ul>
 *   <li>Docker database to be running
 *   <li>map server to be running
 *   <li>map static file server (NGINX) to be running
 * </ul>
 */
@Integration
public class MapUploadIntegrationTest extends MapServerTest {
  private final URI serverUri;
  private final MapUploadClient mapUploadClient;
  private final MapsListingClient mapsListingClient;

  MapUploadIntegrationTest(final URI serverUri) {
    this.serverUri = serverUri;
    mapUploadClient = new MapUploadClient();
    mapsListingClient = new MapsListingClient(serverUri);
  }

  @Test
  @Disabled // Map upload feature is a work-in-progress (Project#17 WIP)
  void uploadTest() throws Exception {
    final Path exampleMap = zipExampleMap();

    final MapUploadResult uploadResult = mapUploadClient.uploadMap(exampleMap);
    final List<MapDownloadListing> mapDownloadListings = mapsListingClient.fetchMapDownloads();

    assertThat(mapDownloadListings, contains(buildExpectedDownloadListing(uploadResult)));
  }

  private Path zipExampleMap() throws Exception {
    final Path exampleMapDir =
        Path.of(
            MapUploadIntegrationTest.class
                .getClassLoader()
                .getResource("example_map_upload")
                .toURI());
    final Path zippedFile = UploadTestUtil.zipDirectory(exampleMapDir);
    zippedFile.toFile().deleteOnExit();
    return zippedFile;
  }

  private MapDownloadListing buildExpectedDownloadListing(final MapUploadResult uploadResult) {
    return MapDownloadListing.builder()
        .description("<p>Example map description</p>")
        .mapCategory("EXPERIMENTAL")
        .mapName("Example Map")
        .previewImage(serverUri.toString() + "/maps/" + uploadResult.getMapId() + "/preview.png")
        .url(serverUri.toString() + "/maps/" + uploadResult.getMapId() + "/example_map.zip")
        .version("1")
        .build();
  }
}
