package org.triplea.http.client.throttle.size;

import org.triplea.http.client.throttle.MessageNotSentException;
import org.triplea.http.data.error.report.ErrorReport;

/**
 * An exception that indicates there were too many characters in the outgoing payload message.
 */
public class MessageExceedsMaxSizeException extends MessageNotSentException {

  private static final long serialVersionUID = -7983918555284517976L;

  MessageExceedsMaxSizeException(final ErrorReport errorReport) {
    super(String.format("Could not send error report message, message is too long (%s characters)",
        errorReport.toString().length()));
  }
}
