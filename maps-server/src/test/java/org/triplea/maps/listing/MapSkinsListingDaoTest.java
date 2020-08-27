package org.triplea.maps.listing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import java.util.List;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.maps.listing.MapSkinListing;
import org.triplea.maps.server.http.MapServerTest;

@DataSet("map_listing/select_map_skins.yml")
@AllArgsConstructor
class MapSkinsListingDaoTest extends MapServerTest {

  private final MapSkinsListingDao mapListingDao;

  @Test
  void verifySelect() {
    final List<MapSkinRecord> results = mapListingDao.fetchMapSkinsListings();

    assertThat(results, hasSize(3));
    // results should be sorted by name

    final MapSkinListing skinListing1 = results.get(0).toMapSkinListing();
    assertThat(skinListing1.getSkinName(), is("map-skin-name-1"));
    assertThat(skinListing1.getUrl(), is("http://map-skin-1"));
    assertThat(skinListing1.getPreviewImageUrl(), is("http://map-thumb-1"));
    assertThat(skinListing1.getDescription(), is("map-skin-description-1"));

    final MapSkinListing skinListing2 = results.get(1).toMapSkinListing();
    assertThat(skinListing2.getSkinName(), is("map-skin-name-2"));
    assertThat(skinListing2.getUrl(), is("http://map-skin-2"));
    assertThat(skinListing2.getPreviewImageUrl(), is("http://map-thumb-2"));
    assertThat(skinListing2.getDescription(), is("map-skin-description-2"));

    final MapSkinListing skinListing3 = results.get(2).toMapSkinListing();
    assertThat(skinListing3.getSkinName(), is("map-skin-name-3"));
    assertThat(skinListing3.getUrl(), is("http://map-skin-3"));
    assertThat(skinListing3.getPreviewImageUrl(), is("http://map-thumb-3"));
    assertThat(skinListing3.getDescription(), is("map-skin-description-3"));
  }
}
