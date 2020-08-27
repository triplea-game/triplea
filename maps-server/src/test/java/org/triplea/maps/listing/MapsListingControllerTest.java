package org.triplea.maps.listing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.maps.listing.MapsListingClient;
import org.triplea.maps.server.http.MapServerTest;

class MapsListingControllerTest extends MapServerTest {

  private final MapsListingClient mapsListingClient;

  MapsListingControllerTest(final URI serverUri) {
    mapsListingClient = new MapsListingClient(serverUri);
  }

  @Test
  void verifyMapListingEndpoint() {
    assertThat(mapsListingClient.fetchMapDownloads(), is(notNullValue()));
  }
}
