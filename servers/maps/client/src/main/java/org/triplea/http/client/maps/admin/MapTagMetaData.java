package org.triplea.http.client.maps.admin;

import java.util.List;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Value;

/**
 * Data about a map tag useful for moderator toolbox. Data drives the ability for moderators to be
 * able to change map tag information.
 */
@Builder
@Value
public class MapTagMetaData {
  @Nonnull String tagName;
  @Nonnull Integer displayOrder;
  @Nonnull List<String> allowedValues;
}
