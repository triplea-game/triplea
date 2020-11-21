package org.triplea.storage.server.client;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UploadResponse {
  private final Exception exception;
  private final String bodyResponse;
  private final Integer statusCode;
}
