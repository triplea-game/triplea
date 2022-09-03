package org.triplea.dropwizard.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IpAddressExtractorTest {

  private static final String IPV4 = "127.0.0.1";
  private static final String IPV6 = "3ffe:1900:4545:3:200:f8ff:fe21:67cf";

  @Mock private HttpServletRequest httpServletRequest;

  @Test
  void ipv6() {
    when(httpServletRequest.getRemoteAddr()).thenReturn(IPV6);
    assertThat(
        "Normal formatted IPv6 as input should return same value",
        IpAddressExtractor.extractIpAddress(httpServletRequest),
        is(IPV6));
  }

  @Test
  void ipv6_WithBrackets() {
    when(httpServletRequest.getRemoteAddr()).thenReturn("[" + IPV6 + "]");
    assertThat(
        "Square brackets on the IPv6 should be stripped",
        IpAddressExtractor.extractIpAddress(httpServletRequest),
        is(IPV6));
  }

  @Test
  void ipv4() {
    when(httpServletRequest.getRemoteAddr()).thenReturn(IPV4);
    assertThat(
        "IPv4 format is returned unaltered",
        IpAddressExtractor.extractIpAddress(httpServletRequest),
        is(IPV4));
  }
}
