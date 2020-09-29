package org.triplea.maps.upload;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UploadResult {
  String messageToUser;
  boolean success;
}
