package org.triplea.test.common;

import com.google.common.base.Preconditions;
import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.experimental.UtilityClass;
import org.hamcrest.Matcher;

/** Set of hamcrest matchers to verify IP addresses are valid and match an expected value. */
@UtilityClass
public class IpAddressMatchers {

  public Matcher<String> ipAddressMatching(final String expected) {
    Preconditions.checkNotNull(expected);
    Preconditions.checkArgument(!expected.isBlank());
    return ipAddressMatching(toInet(expected));
  }

  public Matcher<String> ipAddressMatching(final InetAddress expected) {
    Preconditions.checkNotNull(expected);

    return CustomMatcher.<String>builder()
        .checkCondition(value -> value != null && toInet(value).equals(expected))
        .debug(actual -> "Value was: " + actual)
        .description("Expected value to be: " + expected.getHostAddress())
        .build();
  }

  private static InetAddress toInet(final String ipString) {
    try {
      return InetAddress.getByName(ipString);
    } catch (final UnknownHostException e) {
      throw new IllegalArgumentException("Invalid IP address: '" + ipString + "'", e);
    }
  }
}
