package org.triplea.http.client.error.report.create;

import java.net.URI;
import java.util.Optional;

import com.google.common.base.Strings;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Data object that corresponds to the JSON response from http-server for error report.
 */
@ToString
@Builder
public class ErrorReportResponse {
  /**
   * A link to the github issue created (empty if there were problems creating the link).
   */
  private final String githubIssueLink;

  /**
   * Any errors from server while creating the error report.
   */
  @Getter
  private final String error;


  /**
   * Returns the saved report identifier if the server successfully saved the error report request.
   */
  public Optional<URI> getGithubIssueLink() {
    return Optional.ofNullable(Strings.emptyToNull(githubIssueLink))
        .map(URI::create);
  }
}
