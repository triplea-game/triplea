package org.triplea.http.client;

import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Value;

/**
 * A common response object where the server response is simply a 'yes/no' confirmation with a
 * confirmation message or an error message to the user.
 */
@Value
@Builder
public class GenericServerResponse {
  public static final GenericServerResponse SUCCESS =
      GenericServerResponse.builder().success(true).build();

  /** True if the requested operation was successful. */
  boolean success;

  /**
   * May be null, any message the server wishes to return, eg: if success then this value could be
   * null or a confirmation message, otherwise if not success, then the value should be an error
   * message back to the user.
   */
  @Nullable String message;
}
