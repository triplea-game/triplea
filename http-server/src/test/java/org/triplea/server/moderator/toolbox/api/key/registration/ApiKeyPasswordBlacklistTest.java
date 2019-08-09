package org.triplea.server.moderator.toolbox.api.key.registration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import org.junit.jupiter.api.Test;

class ApiKeyPasswordBlacklistTest {

  private final ApiKeyPasswordBlacklist apiKeyPasswordBlacklist = new ApiKeyPasswordBlacklist();

  @Test
  void testBlacklisted() {
    ApiKeyPasswordBlacklist.PASSWORD_BLACKLIST.forEach(
        blackListed -> assertThat(apiKeyPasswordBlacklist.test(blackListed), is(true)));
  }

  /**
   * In this test we'll get an element we know is on blacklist and verify that it is detected
   * whether upper case or lower case.
   */
  @Test
  void testBlacklistedCasingDoesNotMatter() {
    final String value = ApiKeyPasswordBlacklist.PASSWORD_BLACKLIST.iterator().next();
    assertThat(apiKeyPasswordBlacklist.test(value.toLowerCase()), is(true));
    assertThat(apiKeyPasswordBlacklist.test(value.toUpperCase()), is(true));
  }

  @Test
  void testNotOnBlackList() {
    final String okayPassword = "not on blacklist";
    assumeThat(ApiKeyPasswordBlacklist.PASSWORD_BLACKLIST.contains(okayPassword), is(false));
    assertThat(apiKeyPasswordBlacklist.test(okayPassword), is(false));
  }
}
