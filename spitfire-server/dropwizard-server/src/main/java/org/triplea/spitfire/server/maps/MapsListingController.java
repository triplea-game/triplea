package org.triplea.spitfire.server.maps;

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
import org.triplea.http.client.maps.listing.MapsListingClient;
import org.triplea.maps.listing.MapListingDao;
import org.triplea.maps.listing.MapsListingModule;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MapsListingController {
  private final Supplier<List<MapDownloadItem>> downloadListingSupplier;

  public static MapsListingController build(final Jdbi jdbi) {
    return new MapsListingController(new MapsListingModule(jdbi.onDemand(MapListingDao.class)));
  }

  @GET
  @Path(MapsListingClient.MAPS_LISTING_PATH)
  public List<MapDownloadItem> fetchAvailableMaps() {
    return downloadListingSupplier.get();
  }
}
