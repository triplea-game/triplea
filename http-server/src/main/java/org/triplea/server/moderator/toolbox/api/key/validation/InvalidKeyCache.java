package org.triplea.server.moderator.toolbox.api.key.validation;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;

import org.triplea.server.http.IpAddressExtractor;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;

import lombok.Setter;

/**
 * Essentially a wrapper around a static Guava cache. Provides a nicer API and easier testing.
 */
class InvalidKeyCache {

  @Setter
  private static Cache<String, AtomicInteger> cache;

  InvalidKeyCache() {
    Preconditions.checkNotNull(cache);
  }

  synchronized void increment(final HttpServletRequest request) {
    final String ip = IpAddressExtractor.extractClientIp(request);

    final AtomicInteger integer = Optional.ofNullable(cache.getIfPresent(ip))
        .orElseGet(() -> new AtomicInteger(0));

    integer.incrementAndGet();

    cache.put(ip, integer);
  }

  int getCount(final HttpServletRequest request) {
    final String ip = IpAddressExtractor.extractClientIp(request);
    return Optional.ofNullable(cache.getIfPresent(ip))
        .orElseGet(() -> new AtomicInteger(0))
        .get();
  }

  int totalSum() {
    return cache.asMap().values().stream().mapToInt(AtomicInteger::get).sum();
  }
}
