package org.triplea.web.socket;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class InetExtractor {

  @VisibleForTesting @NonNls
  public static final String IP_ADDRESS_KEY = "javax.websocket.endpoint.remoteAddress";

  /**
   * Returns the IP address of the session from the provided 'userSession' map. It is expected that
   * for the websocket library that we use that we will always find an IP address.
   */
  public static InetAddress extract(final Map<String, Object> userSession) {
    // expected format '/127.0.0.1:42840' or (for test-cases) '127.0.0.1'
    final String rawIpString = String.valueOf(userSession.get(IP_ADDRESS_KEY));

    final String ipString =
        rawIpString.startsWith("/") //
            ? rawIpString.substring(1)
            : rawIpString;

    try {
      final String ip = Splitter.on(':').splitToList(ipString).get(0);
      return InetAddress.getByName(ip);
    } catch (final UnknownHostException e) {
      // not expected
      throw new AssertionError("Unexpected bad hostname: " + userSession.get(IP_ADDRESS_KEY), e);
    }
  }
}
