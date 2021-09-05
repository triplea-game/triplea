package org.triplea.spitfire.server.maps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import java.net.URI;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.http.client.maps.listing.MapsClient;
import org.triplea.spitfire.server.SpitfireDatabaseTestSupport;
import org.triplea.spitfire.server.SpitfireServerTestExtension;

@AllArgsConstructor
@ExtendWith(SpitfireServerTestExtension.class)
@ExtendWith(SpitfireDatabaseTestSupport.class)
@ExtendWith(DBUnitExtension.class)
@DataSet(value = "map_index.yml,map_tag_value.yml", useSequenceFiltering = false)
class MapsControllerTest {
  private final URI localhost;

  /** Invoke the maps listing endpoint and verify response data is present. */
  @Test
  void invokeMapsListingEndpoint() {
    final var result = new MapsClient(localhost).fetchMapDownloads();

    assertThat(result, hasSize(2));
    for (int i = 0; i < 2; i++) {
      assertThat(result.get(i).getDescription(), is(notNullValue()));
      assertThat(result.get(i).getMapName(), is(notNullValue()));
      assertThat(result.get(i).getDownloadUrl(), is(notNullValue()));
      assertThat(result.get(i).getLastCommitDateEpochMilli(), is(notNullValue()));
      assertThat(result.get(i).getPreviewImageUrl(), is(notNullValue()));
    }
    assertThat(result.get(0).getMapTags(), hasSize(2));
    assertThat(
        "Verify tags are sorted by display order",
        result.get(0).getMapTags().get(0).getDisplayOrder()
            < result.get(0).getMapTags().get(1).getDisplayOrder(),
        is(true));

    assertThat(result.get(1).getMapTags(), hasSize(1));
    assertThat(result.get(1).getMapTags().get(0).getName(), is(notNullValue()));
    assertThat(result.get(1).getMapTags().get(0).getValue(), is(notNullValue()));
    assertThat(
        "Display order should always be a positive value",
        result.get(1).getMapTags().get(0).getDisplayOrder() > 0,
        is(true));
  }
}
