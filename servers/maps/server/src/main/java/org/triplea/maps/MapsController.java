package org.triplea.maps;

import java.util.List;
import java.util.function.Supplier;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.http.client.maps.listing.MapDownloadItem;
import org.triplea.http.client.maps.listing.MapsClient;
import org.triplea.maps.listing.MapsListingModule;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MapsController {
  private final Supplier<List<MapDownloadItem>> downloadListingSupplier;

  public static MapsController build(final Jdbi jdbi) {
    return new MapsController(MapsListingModule.build(jdbi));
  }

  /**
   * Returns the full set of maps available for download. This is useful for populating a 'download
   * maps listing', the intended audience is any game user searching for available to download maps.
   */
  @GET
  @Path(MapsClient.MAPS_LISTING_PATH)
  public List<MapDownloadItem> fetchAvailableMaps() {
    return downloadListingSupplier.get();
  }
}
