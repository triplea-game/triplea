package org.triplea.lobby.server.login;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.net.InetAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FailedLoginThrottleTest {

  @Mock private InetAddress address;

  @Test
  void verifyThrottle() {
    final var throttle = new FailedLoginThrottle();

    for (int i = 0; i < FailedLoginThrottle.MAX_FAILED_LOGIN_ATTEMPTS - 1; i++) {
      assertThat(throttle.tooManyFailedLoginAttempts(address), is(false));
      throttle.increment(address);
    }
    assertThat(throttle.tooManyFailedLoginAttempts(address), is(true));
  }
}
