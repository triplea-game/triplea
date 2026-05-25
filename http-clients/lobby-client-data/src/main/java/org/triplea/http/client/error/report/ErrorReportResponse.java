package org.triplea.http.client.error.report;

import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/** Data object that corresponds to the JSON response from lobby-server for error report. */
@ToString
@Builder
@Getter
@EqualsAndHashCode
@AllArgsConstructor
public class ErrorReportResponse {
  /**
   * A link to the github issue created (empty if there were problems creating the link). Server
   * should return a 500 in case there are any problems creating the error report.
   */
  @Nonnull private final String githubIssueLink;
}
