package org.triplea.maps.listing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.maps.listing.MapDownloadItem;
import org.triplea.maps.tags.MapTagsDao;

@ExtendWith(MockitoExtension.class)
class MapsListingModuleTest {
  private static final Instant commitDate1 =
      LocalDateTime.of(1990, 12, 31, 23, 59, 0).toInstant(ZoneOffset.UTC);

  private static final Instant commitDate2 =
      LocalDateTime.of(1999, 1, 30, 12, 59, 0).toInstant(ZoneOffset.UTC);

  private static final Instant commitDate3 =
      LocalDateTime.of(1999, 5, 30, 12, 59, 0).toInstant(ZoneOffset.UTC);

  @Mock private MapListingDao mapListingDao;
  @Mock private MapTagsDao mapTagsDao;

  @InjectMocks private MapsListingModule mapsListingModule;

  @Test
  @DisplayName("Should call map listing DAOs, merge and return data")
  void verifyDataFetch() {
    when(mapListingDao.fetchMapListings())
        .thenReturn(
            List.of(
                MapListingRecord.builder()
                    .downloadUrl("http://map-url-1")
                    .previewImageUrl("http-preview-url-1")
                    .name("map-name-1")
                    .lastCommitDate(commitDate1)
                    .description("description-1")
                    .downloadSizeBytes(10L)
                    .build(),
                MapListingRecord.builder()
                    .downloadUrl("http://map-url-2")
                    .previewImageUrl("http-preview-url-2")
                    .name("map-name-2")
                    .lastCommitDate(commitDate2)
                    .description("description-2")
                    .downloadSizeBytes(20L)
                    .build(),
                MapListingRecord.builder()
                    .downloadUrl("http://map-url-3")
                    .previewImageUrl("http-preview-url-3")
                    .name("map-name-3")
                    .lastCommitDate(commitDate3)
                    .description("description-3")
                    .downloadSizeBytes(30L)
                    .build()));

    when(mapTagsDao.fetchAllMapTags())
        .thenReturn(
            List.of(
                // 2 tags on map-name-1
                MapTagRecord.builder()
                    .mapName("map-name-1")
                    .displayOrder(1)
                    .tagName("tag-name-1")
                    .value("tag-name-1-value")
                    .build(),
                MapTagRecord.builder()
                    .mapName("map-name-1")
                    .displayOrder(2)
                    .tagName("tag-name-2")
                    .value("tag-name-2-value")
                    .build(),
                // 1 tags on map-name-2
                MapTagRecord.builder()
                    .mapName("map-name-2")
                    .displayOrder(1)
                    .tagName("tag-name-1")
                    .value("map-2-tag-name-1-value")
                    .build()
                // 0 tags on map-name-3
                ));

    final List<MapDownloadItem> results = mapsListingModule.get();
    assertThat(results, hasSize(3));
    // expected sort by map name, so first map should be id "1"
    assertThat(results.get(0).getMapName(), is("map-name-1"));
    assertThat(results.get(0).getLastCommitDateEpochMilli(), is(commitDate1.toEpochMilli()));
    assertThat(results.get(0).getDownloadUrl(), is("http://map-url-1"));
    assertThat(results.get(0).getPreviewImageUrl(), is("http-preview-url-1"));
    assertThat(results.get(0).getDownloadSizeInBytes(), is(10L));
    assertThat(results.get(0).getTagValue("tag-name-1"), is("tag-name-1-value"));
    assertThat(results.get(0).getTagValue("tag-name-2"), is("tag-name-2-value"));

    assertThat(results.get(1).getMapName(), is("map-name-2"));
    assertThat(results.get(1).getLastCommitDateEpochMilli(), is(commitDate2.toEpochMilli()));
    assertThat(results.get(1).getDownloadUrl(), is("http://map-url-2"));
    assertThat(results.get(1).getPreviewImageUrl(), is("http-preview-url-2"));
    assertThat(results.get(1).getDownloadSizeInBytes(), is(20L));

    assertThat(results.get(2).getMapName(), is("map-name-3"));
    assertThat(results.get(2).getLastCommitDateEpochMilli(), is(commitDate3.toEpochMilli()));
    assertThat(results.get(2).getDownloadUrl(), is("http://map-url-3"));
    assertThat(results.get(2).getPreviewImageUrl(), is("http-preview-url-3"));
    assertThat(results.get(2).getDownloadSizeInBytes(), is(30L));
  }
}
