package org.triplea.http.client.error.report;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString
@Builder
@Getter
@EqualsAndHashCode
public class CanUploadErrorReportResponse {
  /**
   * True means a user can upload an error report. False means an error report is already uploaded.
   * If true, then responseDetails and existingBugReportUrl will be null.
   */
  @Nonnull private final Boolean canUpload;

  /**
   * Contains any message details that should be displayed to the user. EG: "This error is already
   * uploaded"
   */
  @Nullable private final String responseDetails;

  /** Contains a link to any existing error report that matches the same error the user sees. */
  @Nullable private final String existingBugReportUrl;
}
