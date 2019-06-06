package org.triplea.server.moderator.toolbox.api.key.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.cache.Cache;

@ExtendWith(MockitoExtension.class)
class InvalidKeyCacheTest {

  private static final String IP_ADDRESS_1 = "Grogs are the cannibals of the big urchin.";
  private static final String IP_ADDRESS_2 = "Damn yer parrot, feed the sea-dog.";
  private static final String IP_ADDRESS_3 = "Jolly roger, yer not blowing me without a fortune!";


  @Mock
  private Cache<String, AtomicInteger> cache;

  @Mock
  private HttpServletRequest httpServletRequest;

  private InvalidKeyCache invalidKeyCache;

  @BeforeEach
  void setup() {
    InvalidKeyCache.setCache(cache);
    invalidKeyCache = new InvalidKeyCache();
  }

  @AfterEach
  void tearDown() {
    reset(cache);
  }


  @Test
  void incrementWithNewKey() {
    when(httpServletRequest.getRemoteHost()).thenReturn(IP_ADDRESS_1);
    when(cache.getIfPresent(IP_ADDRESS_1)).thenReturn(null);

    invalidKeyCache.increment(httpServletRequest);

    verify(cache).put(IP_ADDRESS_1, new AtomicInteger(1));
  }

  @Test
  void incrementWithExistingKey() {
    when(httpServletRequest.getRemoteHost()).thenReturn(IP_ADDRESS_1);
    when(cache.getIfPresent(IP_ADDRESS_1)).thenReturn(new AtomicInteger(1));

    invalidKeyCache.increment(httpServletRequest);

    verify(cache).put(IP_ADDRESS_1, new AtomicInteger(1));
  }

  @Test
  void getCountWithNewKey() {
    when(httpServletRequest.getRemoteHost()).thenReturn(IP_ADDRESS_1);
    when(cache.getIfPresent(IP_ADDRESS_1)).thenReturn(null);

    assertThat(invalidKeyCache.getCount(httpServletRequest), is(0));
  }

  @Test
  void getCountWithExistingKey() {
    when(httpServletRequest.getRemoteHost()).thenReturn(IP_ADDRESS_1);
    when(cache.getIfPresent(IP_ADDRESS_1)).thenReturn(new AtomicInteger(1));

    assertThat(invalidKeyCache.getCount(httpServletRequest), is(1));
  }

  @Test
  void totalSumWithNoEntries() {
    final ConcurrentHashMap<String, AtomicInteger> map = new ConcurrentHashMap<>();
    map.put(IP_ADDRESS_1, new AtomicInteger(5));
    map.put(IP_ADDRESS_2, new AtomicInteger(10));
    map.put(IP_ADDRESS_3, new AtomicInteger(15));
    when(cache.asMap()).thenReturn(map);

    assertThat(invalidKeyCache.totalSum(), is(30));
  }
}
