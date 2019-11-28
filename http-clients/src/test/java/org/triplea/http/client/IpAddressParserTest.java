package org.triplea.http.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.InetAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IpAddressParserTest {
  @ParameterizedTest
  @ValueSource(
      strings = {
        "99.99.0.1", //
        "fe80::ad:ad:5e4f:58a2",
        "2602:602:f00:ed0:5d12:e3b4:a4d7:c2ea",
        "2602:603:f00:ed0::2"
      })
  @DisplayName("Verify can parse IPv4 and Ipv6 address")
  void parseAddress(final String toParse) throws Exception {
    final InetAddress expected = InetAddress.getByName(toParse);

    final InetAddress result = IpAddressParser.fromString(toParse);

    assertThat(result, is(expected));
  }

  @Test
  @DisplayName("Null input throws NPE")
  void throwsNullPointerOnNullInput() {
    assertThrows(NullPointerException.class, () -> IpAddressParser.fromString(null));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "", //
        "not-an-ip",
        "99.99.99.99.99"
      })
  @DisplayName("Verify that invalid IP address throw IllegalArgument")
  void throwsIllegalArgOnInvalidIpAddress(final String invalid) throws Exception {
    assertThrows(IllegalArgumentException.class, () -> IpAddressParser.fromString(invalid));
  }
}
