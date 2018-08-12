package games.strategy.engine.framework.startup.login;

import static games.strategy.test.Assertions.assertNotThrows;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import games.strategy.engine.framework.startup.login.Md5CryptAuthenticator.ChallengePropertyNames;
import games.strategy.engine.framework.startup.login.Md5CryptAuthenticator.ResponsePropertyNames;
import games.strategy.util.Md5Crypt;

final class Md5CryptAuthenticatorTest {
  private static final String PASSWORD = "←PASSWORD↑WITH→UNICODE↓CHARS";

  private static Map<String, String> newChallengeWithAllProperties() {
    return Maps.newHashMap(ImmutableMap.of(
        ChallengePropertyNames.SALT, ""));
  }

  private static Map<String, String> newChallengeWithAllPropertiesExcept(final String name) {
    final Map<String, String> challenge = newChallengeWithAllProperties();
    challenge.remove(name);
    return challenge;
  }

  private static Map<String, String> newResponseWithAllProperties() {
    return Maps.newHashMap(ImmutableMap.of(
        ResponsePropertyNames.DIGEST, ""));
  }

  private static Map<String, String> newResponseWithAllPropertiesExcept(final String name) {
    final Map<String, String> response = newResponseWithAllProperties();
    response.remove(name);
    return response;
  }

  @Nested
  final class CanProcessChallengeTest {
    @Test
    void shouldReturnTrueWhenAllPropertiesPresent() {
      final Map<String, String> challenge = newChallengeWithAllProperties();

      assertThat(Md5CryptAuthenticator.canProcessChallenge(challenge), is(true));
    }

    @Test
    void shouldReturnFalseWhenSaltAbsent() {
      final Map<String, String> challenge = newChallengeWithAllPropertiesExcept(ChallengePropertyNames.SALT);

      assertThat(Md5CryptAuthenticator.canProcessChallenge(challenge), is(false));
    }
  }

  @Nested
  final class NewChallengeTest {
    @Test
    void shouldIncludeSalt() {
      final Map<String, String> challenge = Md5CryptAuthenticator.newChallenge();

      assertThat(challenge, hasEntry(is(ChallengePropertyNames.SALT), is(not(emptyString()))));
    }
  }

  @Nested
  final class CanProcessResponseTest {
    @Test
    void shouldReturnTrueWhenAllPropertiesPresent() {
      final Map<String, String> response = newResponseWithAllProperties();

      assertThat(Md5CryptAuthenticator.canProcessResponse(response), is(true));
    }

    @Test
    void shouldReturnFalseWhenDigestAbsent() {
      final Map<String, String> response = newResponseWithAllPropertiesExcept(ResponsePropertyNames.DIGEST);

      assertThat(Md5CryptAuthenticator.canProcessResponse(response), is(false));
    }
  }

  @Nested
  final class NewResponseTest {
    @Test
    void shouldIncludeResponseWhenChallengeContainsSalt() throws Exception {
      final Map<String, String> challenge = ImmutableMap.of(
          ChallengePropertyNames.SALT, Md5Crypt.newSalt());

      final Map<String, String> response = Md5CryptAuthenticator.newResponse(PASSWORD, challenge);

      assertThat(response, hasEntry(is(ResponsePropertyNames.DIGEST), is(not(emptyString()))));
    }

    @Test
    void shouldThrowExceptionWhenChallengeDoesNotContainSalt() {
      final Map<String, String> challenge = ImmutableMap.of();

      final Exception e =
          assertThrows(AuthenticationException.class, () -> Md5CryptAuthenticator.newResponse(PASSWORD, challenge));
      assertThat(e.getMessage(), containsString("missing"));
    }
  }

  @Nested
  final class GetRequiredPropertyTest {
    @Test
    void shouldReturnValueWhenNamePresent() throws Exception {
      final String name = "name";
      final String value = "value";
      final Map<String, String> properties = ImmutableMap.of(name, value);

      final String actualValue = Md5CryptAuthenticator.getRequiredProperty(properties, name);

      assertThat(actualValue, is(value));
    }

    @Test
    void shouldThrowExceptionWhenNameAbsent() {
      final String name = "name";
      final Map<String, String> properties = ImmutableMap.of("other name", "value");

      final Exception e = assertThrows(AuthenticationException.class,
          () -> Md5CryptAuthenticator.getRequiredProperty(properties, name));

      assertThat(e.getMessage(), allOf(
          containsString("missing"),
          containsString(name)));
    }
  }

  @Nested
  final class AuthenticateTest {
    @Test
    void shouldNotThrowExceptionWhenAuthenticationSucceeds() throws Exception {
      final Map<String, String> challenge = Md5CryptAuthenticator.newChallenge();
      final Map<String, String> response = Md5CryptAuthenticator.newResponse(PASSWORD, challenge);

      assertNotThrows(() -> Md5CryptAuthenticator.authenticate(PASSWORD, challenge, response));
    }

    @Test
    void shouldThrowExceptionWhenAuthenticationFails() throws Exception {
      final Map<String, String> challenge = Md5CryptAuthenticator.newChallenge();
      final Map<String, String> response = Md5CryptAuthenticator.newResponse("otherPassword", challenge);

      final Exception e = assertThrows(AuthenticationException.class,
          () -> Md5CryptAuthenticator.authenticate(PASSWORD, challenge, response));

      assertThat(e.getMessage(), containsString("authentication failed"));
    }

    @Test
    void shouldThrowExceptionWhenChallengeDoesNotContainSalt() throws Exception {
      final Map<String, String> challenge = Md5CryptAuthenticator.newChallenge();
      final Map<String, String> response = Md5CryptAuthenticator.newResponse(PASSWORD, challenge);

      challenge.remove(ChallengePropertyNames.SALT);

      final Exception e = assertThrows(AuthenticationException.class,
          () -> Md5CryptAuthenticator.authenticate(PASSWORD, challenge, response));

      assertThat(e.getMessage(), allOf(
          containsString("missing"),
          containsString(ChallengePropertyNames.SALT)));
    }

    @Test
    void shouldThrowExceptionWhenResponseDoesNotContainDigest() throws Exception {
      final Map<String, String> challenge = Md5CryptAuthenticator.newChallenge();
      final Map<String, String> response = Md5CryptAuthenticator.newResponse(PASSWORD, challenge);

      response.remove(ResponsePropertyNames.DIGEST);

      final Exception e = assertThrows(AuthenticationException.class,
          () -> Md5CryptAuthenticator.authenticate(PASSWORD, challenge, response));
      assertThat(e.getMessage(), allOf(
          containsString("missing"),
          containsString(ResponsePropertyNames.DIGEST)));
    }
  }
}
