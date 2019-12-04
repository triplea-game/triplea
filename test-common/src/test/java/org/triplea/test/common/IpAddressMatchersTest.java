package org.triplea.test.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.triplea.test.common.IpAddressMatchers.ipAddressMatching;

import java.net.InetAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IpAddressMatchersTest {

  private static final String IP = "99.99.99.99";

  @Test
  @DisplayName("Verify null IP as value will not match")
  void nullValueNeverMatches() throws Exception {
    assertThrows(AssertionError.class, () -> assertThat(null, is(ipAddressMatching(IP))));
    assertThrows(
        AssertionError.class,
        () -> assertThat(null, is(ipAddressMatching(InetAddress.getByName(IP)))));
  }

  @Test
  @DisplayName("Verify null IP as expected will throw")
  void nullExpectedValueThrows() {
    assertThrows(
        NullPointerException.class, () -> assertThat(IP, is(ipAddressMatching((String) null))));
    assertThrows(
        NullPointerException.class,
        () -> assertThat(IP, is(ipAddressMatching((InetAddress) null))));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "", //
        "0.0.0.0.0",
        "/99.99.99.99",
        "2602:602:f00:ed0:5d12:e3b4:a4d7:c2ea:111:11:11"
      })
  @DisplayName("Expecting an invalid IP is an error, throws illegal argument")
  void matchingArgumentMustBeValidIp(final String invalid) {
    assertThrows(
        IllegalArgumentException.class, () -> assertThat(IP, is(ipAddressMatching(invalid))));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "99.99.0.1", //
        "fe80::ad:ad:5e4f:58a2",
        "2602:602:f00:ed0:5d12:e3b4:a4d7:c2ea",
        "2602:603:f00:ed0::2"
      })
  @DisplayName("Verify case where IP addresses simply do not match")
  void mismatchingIp(final String ip) {
    assertThrows(AssertionError.class, () -> assertThat(ip, is(ipAddressMatching(IP))));
    assertThrows(
        AssertionError.class,
        () -> assertThat(ip, is(ipAddressMatching(InetAddress.getByName(IP)))));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "99.99.0.1", //
        "fe80::ad:ad:5e4f:58a2",
        "2602:602:f00:ed0:5d12:e3b4:a4d7:c2ea",
        "2602:603:f00:ed0::2"
      })
  @DisplayName("Happy path, verify matching IP addresses pass")
  void matchingIpValues(final String ip) throws Exception {
    assertThat(ip, is(ipAddressMatching(ip)));
    assertThat(ip, is(ipAddressMatching(InetAddress.getByName(ip))));
  }
}
