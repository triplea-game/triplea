package org.triplea.server.error.reporting.upload;

import lombok.Builder;
import lombok.Value;

/** Parameter object for inserting a record of an error report uploaded by a user. */
@Value
@Builder
public class InsertHistoryRecordParams {
  /** The IP address of the user reporting the error. */
  String ip;

  /** SystemId of the user uploading the error report. */
  String systemId;

  /** The engine version of the user at the time they uploaded the error report. */
  String gameVersion;

  /** The title of the error report, will be used for de-duping. */
  String title;

  /**
   * Link to the github issue created by the error report. This is the issue that the user should
   * have just uploaded.
   */
  String githubIssueLink;
}
