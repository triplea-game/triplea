package org.triplea.server;

import java.net.URI;

import org.triplea.http.client.github.issues.IssueClientParams;
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
    return builder().errorUploader(
        ErrorUploadConfiguration.newErrorUploader(
            IssueClientParams.builder()
                .authToken(EnvironmentVariable.GITHUB_API_AUTH_TOKEN.getValue())
                .githubOrg(EnvironmentVariable.ERROR_REPORTING_GITHUB_ORG.getValue())
                .githubRepo(EnvironmentVariable.ERROR_REPORTING_GITHUB_REPO.getValue())
                .uri(URI.create("https://api.github.com"))
                .build()))
        .build();
  }
}
