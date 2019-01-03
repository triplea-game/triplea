package org.triplea.server;

import org.triplea.server.reporting.error.ErrorUploadConfiguration;
import org.triplea.server.reporting.error.ErrorUploadStrategy;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Dependency injection layer that wires together the business layer beans. These beans will be used by
 * controllers to then provide the functionality behind http endpoints.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ServerConfiguration {
  private final ErrorUploadStrategy errorUploader;

  public static ServerConfiguration prod() {
    return configure(EnvironmentConfiguration.prod());
  }

  private static ServerConfiguration configure(final EnvironmentConfiguration config) {
    return builder()
        .errorUploader(new ErrorUploadConfiguration(config).newErrorUploader())
        .build();
  }
}
