package games.strategy.engine.framework.map.listing;

import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class MapDownloadListing {
  @Nonnull private final String url;
  @Nonnull private final String mapName;
  @Nonnull private final String description;
  @Nonnull private final String version;
  @Nonnull private final String mapCategory;
  private final String previewImage;

  // private final Set<MapSkinListing> mapsSkins;
}
