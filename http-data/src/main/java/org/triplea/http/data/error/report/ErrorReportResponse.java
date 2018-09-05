package org.triplea.http.data.error.report;

import java.util.Optional;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import lombok.Builder;
import lombok.Getter;

/**
 * Data object that corresponds to the JSON response from http-server for error reporting.
 */
@Builder
@Getter
public class ErrorReportResponse {
  @VisibleForTesting
  public static final String SUCCESS = "SUCCESS";
  public static final ErrorReportResponse SUCCESS_RESPONSE = ErrorReportResponse.builder()
      .result(SUCCESS)
      .build();

  /**
   * A status message String, expected to be 'SUCCESS' or 'FAILURE', with 'FAILURE' meaning
   * the error report was not saved on the server.
   */
  private final String result;

  /**
   * A public identifier reported back to the user to link the report back to what we saved
   * on the server.
   */
  private final String savedReportId;


  /**
   * True if the error report was sent and saved by the server.
   */
  public boolean isSuccess() {
    return SUCCESS.equals(result);
  }

  /**
   * Returns the saved report identifier if the server successfully saved the error report request.
   */
  public Optional<String> getSavedReportId() {
    return Optional.ofNullable(Strings.emptyToNull(savedReportId));
  }
}
