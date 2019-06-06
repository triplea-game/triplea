package org.triplea.server.moderator.toolbox.api.key.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvalidKeyLockOutTest {


  private static final int MAX_FAILS_BY_IP = 2;
  private static final int MAX_TOTAL_FAILS = 3;


  @Mock
  private InvalidKeyCache invalidKeyCache;

  private InvalidKeyLockOut invalidKeyLockOut;

  @Mock
  private HttpServletRequest httpServletRequest;

  @BeforeEach
  void setup() {
    invalidKeyLockOut = InvalidKeyLockOut.builder()
        .invalidKeyCache(invalidKeyCache)
        .maxFailsByIpAddress(MAX_FAILS_BY_IP)
        .maxTotalFails(MAX_TOTAL_FAILS)
        .build();
  }

  @Test
  void recordInvalid() {
    invalidKeyLockOut.recordInvalid(httpServletRequest);

    verify(invalidKeyCache).increment(httpServletRequest);
  }

  @Test
  void notLockedOut() {
    when(invalidKeyCache.getCount(httpServletRequest)).thenReturn(MAX_FAILS_BY_IP - 1);
    when(invalidKeyCache.totalSum()).thenReturn(MAX_TOTAL_FAILS - 1);

    assertThat(invalidKeyLockOut.isLockedOut(httpServletRequest), is(false));
  }

  @Test
  void lockedOutByIp() {
    when(invalidKeyCache.getCount(httpServletRequest)).thenReturn(MAX_FAILS_BY_IP);

    assertThat(invalidKeyLockOut.isLockedOut(httpServletRequest), is(true));
  }

  @Test
  void lockedOutByMaxFails() {
    when(invalidKeyCache.getCount(httpServletRequest)).thenReturn(MAX_FAILS_BY_IP);
    when(invalidKeyCache.totalSum()).thenReturn(MAX_FAILS_BY_IP);

    assertThat(invalidKeyLockOut.isLockedOut(httpServletRequest), is(true));
  }
}
