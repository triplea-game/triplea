package org.triplea.http.client.github.issues;

import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.triplea.http.client.error.report.ErrorReportRequest;

/** Parameter/Data object used to construct a {@code GithubIssueClient}. */
@Getter(AccessLevel.PACKAGE)
@Builder
// TODO: rename to: CreateIssueRequest
public class IssueClientParams {
  /** Github Personal access token with repo permissions. */
  @Nonnull private final String authToken;
  /** The name of the github org, used as part of URL. */
  @Nonnull private final String githubOrg;
  /** The name of the github repo, used as part of URL. */
  @Nonnull private final String githubRepo;
  /**
   * Error report sent from the TripleA client, we will forward this to github create issue web
   * service.
   */
  @Nonnull private final ErrorReportRequest errorReportRequest;
}
