package org.triplea.server;

import org.triplea.server.reporting.error.ErrorUploadConfiguration;
import org.triplea.server.reporting.error.ErrorUploadStrategy;

import lombok.Builder;
import lombok.Getter;

/**
 * Dependency injection layer that wires together the business layer beans. These beans will be used
 * by controllers to then provide the functionality behind http endpoints.
 */
@Getter
@Builder
public class ServerConfiguration {
  private final ErrorUploadStrategy errorUploader;

  public static ServerConfiguration prod() {
    return builder().errorUploader(ErrorUploadConfiguration.newErrorUploader()).build();
  }
}
