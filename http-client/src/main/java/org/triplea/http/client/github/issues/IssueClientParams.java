package org.triplea.http.client.github.issues;

import java.net.URI;

import javax.annotation.Nonnull;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

/**
 * Parameter/Data object used to construct a {@code GithubIssueClient}.
 */
@Getter(AccessLevel.PACKAGE)
@Builder
public class IssueClientParams {
  /** Github Personal access token with repo permissions. */
  @Nonnull
  private final String authToken;
  /** The name of the github org, used as part of URL. */
  @Nonnull
  private final String githubOrg;
  /** The name of the github repo, used as part of URL. */
  @Nonnull
  private final String githubRepo;
  /** URI of github web service API. */
  @Nonnull
  private final URI uri;
}
