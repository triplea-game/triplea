package org.triplea.http.client.lobby;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

/**
 * Utility class for creating the headers that we send to the lobby to notify which client version
 * is connecting. This header should be sent to the lobby on every request (including websockets).
 */
@UtilityClass
public class ClientVersionHeader {
  public static final String HEADER_KEY = "Triplea-Version";

  /** Returns a mutable map containing headers with the specified engine version. */
  public static Map<String, String> buildHeader(String version) {
    var headers = new HashMap<String, String>();
    headers.put(HEADER_KEY, version);
    return headers;
  }
}
