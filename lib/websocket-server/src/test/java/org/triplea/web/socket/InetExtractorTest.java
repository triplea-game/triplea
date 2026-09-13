package org.triplea.web.socket;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import java.util.Map;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.Test;

class InetExtractorTest {

  @NonNls private static final String IP_ADDRESS = "127.0.0.1";

  @Test
  void verify() throws Exception {
    final Map<String, Object> propertiesMap =
        Map.of(InetExtractor.IP_ADDRESS_KEY, "/" + IP_ADDRESS + ":42840");

    assertThat(InetExtractor.extract(propertiesMap)).isEqualTo(InetAddress.getByName(IP_ADDRESS));
  }

  @Test
  void verifyCanParseSimpleAddress() throws Exception {
    final Map<String, Object> propertiesMap = Map.of(InetExtractor.IP_ADDRESS_KEY, IP_ADDRESS);

    assertThat(InetExtractor.extract(propertiesMap)).isEqualTo(InetAddress.getByName(IP_ADDRESS));
  }
}
