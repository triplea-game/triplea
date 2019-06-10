package org.triplea.server.moderator.toolbox.api.key.validation;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.triplea.server.http.AppConfig;
import org.triplea.server.http.IpAddressExtractor;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import lombok.Setter;

/**
 * Essentially a wrapper around a static Guava cache. Provides a nicer API and easier testing.
 */
class InvalidKeyCache {
  static {
    InvalidKeyCache.setCache(
        CacheBuilder.newBuilder()
            .expireAfterWrite(AppConfig.FAILED_API_KEY_CACHE_EXPIRATION, TimeUnit.MINUTES)
            .build());
  }

  @Setter(onMethod_ = {@VisibleForTesting})
  private static Cache<String, Integer> cache;

  InvalidKeyCache() {
    Preconditions.checkNotNull(cache);
  }

  void increment(final HttpServletRequest request) {
    final String ip = IpAddressExtractor.extractClientIp(request);

    final int integer = Optional.ofNullable(cache.getIfPresent(ip))
        .orElse(0);

    cache.put(ip, integer + 1);
  }

  int getCount(final HttpServletRequest request) {
    final String ip = IpAddressExtractor.extractClientIp(request);
    return Optional.ofNullable(cache.getIfPresent(ip))
        .orElse(0);
  }

  int totalSum() {
    return cache.asMap().values().stream().mapToInt(i -> i).sum();
  }
}
