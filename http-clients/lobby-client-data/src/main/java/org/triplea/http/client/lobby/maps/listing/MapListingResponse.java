package org.triplea.http.client.lobby.maps.listing;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@AllArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class MapListingResponse {
  List<MapDownloadItem> maps;
}
