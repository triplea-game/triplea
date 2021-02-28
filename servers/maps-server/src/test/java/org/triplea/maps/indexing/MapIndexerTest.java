package org.triplea.maps.indexing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

import java.io.InputStream;
import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.triplea.test.common.StringToInputStream;

class MapIndexerTest {

  @Test
  void ymlIndexing() {
    final InputStream inputStream =
        StringToInputStream.asInputStream("map_name: map_name\nversion: 2");

    final MapIndexResult mapIndexResult =
        MapIndexer.indexMapYmlContent(URI.create("http://uri"), inputStream);

    assertThat(
        mapIndexResult,
        is(
            MapIndexResult.builder()
                .mapRepoUri("http://uri")
                .mapName("map_name")
                .mapVersion(2)
                .build()));
  }

  @DisplayName("Verify that if the indexer finds missing data it returns null")
  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "map_name: missing_version",
        "version: 2",
        "map_name: bad_version_number\nversion: NaN",
        "map_name: bad_indentation\n  version: 2",
        "map_name:  \nversion: 2",
        "map_name: map_name\nversion: ",
      })
  void ymlIndexingInvalidCases(final String inputString) {

    final InputStream inputStream = StringToInputStream.asInputStream(inputString);

    final MapIndexResult mapIndexResult =
        MapIndexer.indexMapYmlContent(URI.create("http://uri"), inputStream);

    assertThat(mapIndexResult, is(nullValue()));
  }
}
