package org.triplea.http.client;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.triplea.http.client.throttle.rate.RateLimitingThrottle;
import org.triplea.http.client.throttle.size.MessageSizeThrottle;


class RateLimiter<RequestT> implements Consumer<RequestT> {

  private final List<Consumer<RequestT>> throttleRules = Arrays.asList(
      new MessageSizeThrottle<>(),
      new RateLimitingThrottle<>());

  @Override
  public void accept(final RequestT requestT) {
    throttleRules.forEach(rule -> rule.accept(requestT));
  }
}
