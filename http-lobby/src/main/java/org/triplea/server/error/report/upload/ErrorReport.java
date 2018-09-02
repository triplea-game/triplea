package org.triplea.server.error.report.upload;

import java.time.Instant;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;


/**
 * A data object with the full details of an error report that we can record on server side for
 * future reference and investigation.
 */
@Builder
@ToString
@EqualsAndHashCode
public class ErrorReport {
  /**
   * An identifier for the reporting machine.
   */
  private final String reportingHostId;
  /**
   * The time when the report was uploaded to our backend server.
   */
  private final Instant reportedOn;
  /**
   * An unstructured payload of the report contents.
   */
  private final String reportContents;

}
