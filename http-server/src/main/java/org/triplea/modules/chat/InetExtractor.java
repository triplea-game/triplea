package org.triplea.modules.chat;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class InetExtractor {

  @VisibleForTesting
  public static final String IP_ADDRESS_KEY = "javax.websocket.endpoint.remoteAddress";

  @SuppressWarnings("UnstableApiUsage")
  public static InetAddress extract(final Map<String, Object> userSession) {
    // expected format '/127.0.0.1:42840'
    final String ipString = String.valueOf(userSession.get(IP_ADDRESS_KEY)).substring(1);
    try {
      final String ip = Splitter.on(':').splitToList(ipString).get(0);

      return InetAddress.getByName(ip);
    } catch (final UnknownHostException e) {
      // not expected
      throw new AssertionError("Unexpected bad hostname: " + userSession.get(IP_ADDRESS_KEY), e);
    }
  }
}
