package org.triplea.http.client.throttle.size;

import java.util.function.Consumer;

import org.triplea.http.client.error.report.json.message.ErrorReport;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Checks size of a single message and if too large throws an exception.
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageSizeThrottle implements Consumer<ErrorReport> {

  /**
   * An upper bound where we will no longer send a message if it is too long.
   * The unit is character count, which is not quite the send sizse, but good enough for our
   * purpose of bounding message size. The upper bound is meant to be generous and really prevent
   * a programming error from sending a really large payload.
   */
  private static final int DEFAULT_CHARACTER_LIMIT = 32_000;

  private final int characterLimit;


  public MessageSizeThrottle() {
    this(DEFAULT_CHARACTER_LIMIT);
  }

  @Override
  public void accept(final ErrorReport errorReport) {
    if (errorReport.toString().length() > characterLimit) {
      throw new MessageExceedsMaxSizeException(errorReport);
    }
  }
}

