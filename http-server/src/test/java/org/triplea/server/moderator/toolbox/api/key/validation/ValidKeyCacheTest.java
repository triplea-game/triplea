package org.triplea.server.moderator.toolbox.api.key.validation;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;


class ValidKeyCacheTest {
  private static final String IP_ADDRESS_1 = "The plank fires with death, drink the captain's quarters until it sings.";
  private static final String IP_ADDRESS_2 = "Aw, golly gosh.";

  private static final String KEY = "Where is the evil sea-dog?";
  private static final int MODERATOR_ID = 22;

  private ValidKeyCache validKeyCache = new ValidKeyCache();

  @Test
  void keyNotFound() {
    assertThat(validKeyCache.get("Faith ho! hail to be commanded."), isEmpty());
  }

  @Test
  void keyFound() {
    validKeyCache.recordValid(KEY, MODERATOR_ID);
    assertThat(validKeyCache.get(KEY), isPresentAndIs(MODERATOR_ID));
  }
}
