package org.triplea.http.client.github.issues.create;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Data object that is converted to JSON and sent to github to create a new issue.
 */
@Builder
@EqualsAndHashCode
@Getter
public class CreateIssueRequest {
  private final String title;
  private final String body;
}
