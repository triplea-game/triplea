package org.triplea.maps.tags;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class UpdateMapTagResult {
  boolean success;
  String message;
}
