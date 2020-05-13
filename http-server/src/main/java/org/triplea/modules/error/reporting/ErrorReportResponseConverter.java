package org.triplea.modules.error.reporting;

import com.google.common.base.Strings;
import java.util.function.Function;
import org.triplea.http.client.error.report.ErrorReportResponse;
import org.triplea.http.client.github.issues.CreateIssueResponse;

/**
 * Converts a response from Github.com by our http-server into a response object we can send back to
 * the TripleA game-client.
 */
public class ErrorReportResponseConverter
    implements Function<CreateIssueResponse, ErrorReportResponse> {

  @Override
  public ErrorReportResponse apply(final CreateIssueResponse response) {
    return ErrorReportResponse.builder().githubIssueLink(extractLink(response)).build();
  }

  private static String extractLink(final CreateIssueResponse response) {
    final String urlInResponse = response.getHtmlUrl();

    if (Strings.emptyToNull(urlInResponse) == null) {
      throw new CreateErrorReportException("Error report link missing from server response");
    }
    return urlInResponse;
  }
}
