package org.triplea.maps.upload;

import java.net.URI;
import org.triplea.http.client.maps.listing.MapsListingClient;
import org.triplea.http.client.maps.upload.MapUploadClient;
import org.triplea.maps.server.http.MapServerTest;
import org.triplea.test.common.Integration;

@Integration
public class MapUploadIntegrationTest  extends MapServerTest {
  MapUploadClient mapUploadClient;


  MapUploadIntegrationTest(final URI serverUri) {
    mapUploadClient = new MapUploadClient();
  }


  void uploadTest() {

  }
}
