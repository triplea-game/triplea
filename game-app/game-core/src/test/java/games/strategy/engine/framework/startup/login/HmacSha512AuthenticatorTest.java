package games.strategy.engine.framework.startup.login;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableMap;
import games.strategy.engine.framework.startup.login.HmacSha512Authenticator.ChallengePropertyNames;
import games.strategy.engine.framework.startup.login.HmacSha512Authenticator.ResponsePropertyNames;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class HmacSha512AuthenticatorTest {
  @NonNls private static final String PASSWORD = "←PASSWORD↑WITH→UNICODE↓CHARS";

  private static String newBase64String() {
    final byte[] bytes = new byte[8];
    Arrays.fill(bytes, (byte) 1);
    return Base64.getEncoder().encodeToString(bytes);
  }

  @Nested
  final class NewChallengeTest {
    @Test
    void shouldIncludeNonceAndSalt() {
      final Map<String, String> challenge = HmacSha512Authenticator.newChallenge();

      assertThat(challenge, hasEntry(is(ChallengePropertyNames.NONCE), is(not(emptyString()))));
      assertThat(challenge, hasEntry(is(ChallengePropertyNames.SALT), is(not(emptyString()))));
    }
  }

  @Nested
  final class NewResponseTest {
    @Test
    void shouldIncludeResponseWhenChallengeContainsNonceAndSalt() throws Exception {
      final Map<String, String> challenge =
          ImmutableMap.of(
              ChallengePropertyNames.NONCE, newBase64String(),
              ChallengePropertyNames.SALT, newBase64String());

      final Map<String, String> response = HmacSha512Authenticator.newResponse(PASSWORD, challenge);

      assertThat(response, hasEntry(is(ResponsePropertyNames.DIGEST), is(not(emptyString()))));
    }

    @Test
    void shouldNotIncludeResponseWhenChallengeDoesNotContainNonce() throws Exception {
      final Map<String, String> challenge =
          ImmutableMap.of(ChallengePropertyNames.SALT, newBase64String());

      final Map<String, String> response = HmacSha512Authenticator.newResponse(PASSWORD, challenge);

      assertThat(response, not(hasKey(ResponsePropertyNames.DIGEST)));
    }

    @Test
    void shouldNotIncludeResponseWhenChallengeDoesNotContainSalt() throws Exception {
      final Map<String, String> challenge =
          ImmutableMap.of(ChallengePropertyNames.NONCE, newBase64String());

      final Map<String, String> response = HmacSha512Authenticator.newResponse(PASSWORD, challenge);

      assertThat(response, not(hasKey(ResponsePropertyNames.DIGEST)));
    }
  }

  @Nested
  final class DecodeOptionalPropertyTest {
    @Test
    void shouldReturnDecodedValueWhenNamePresent() throws Exception {
      final String name = "name";
      final String encodedValue = newBase64String();
      final Map<String, String> properties = ImmutableMap.of(name, encodedValue);

      final byte[] actualValue = HmacSha512Authenticator.decodeOptionalProperty(properties, name);

      assertThat(actualValue, is(Base64.getDecoder().decode(encodedValue)));
    }

    @Test
    void shouldReturnNullWhenNameAbsent() throws Exception {
      final Map<String, String> properties = ImmutableMap.of("other name", newBase64String());

      final byte[] value = HmacSha512Authenticator.decodeOptionalProperty(properties, "name");

      assertThat(value, is(nullValue()));
    }

    @Test
    void shouldThrowExceptionWhenValueMalformed() {
      final String name = "name";
      final Map<String, String> properties = ImmutableMap.of(name, "NOT_A_BASE64_VALUE");

      final Exception e =
          assertThrows(
              AuthenticationException.class,
              () -> HmacSha512Authenticator.decodeOptionalProperty(properties, name));
      assertThat(e.getMessage(), allOf(containsString("malformed"), containsString(name)));
    }
  }

  @Nested
  final class AuthenticateTest {
    @Test
    void shouldNotThrowExceptionWhenAuthenticationSucceeds() throws Exception {
      final Map<String, String> challenge = HmacSha512Authenticator.newChallenge();
      final Map<String, String> response = HmacSha512Authenticator.newResponse(PASSWORD, challenge);

      assertDoesNotThrow(() -> HmacSha512Authenticator.authenticate(PASSWORD, challenge, response));
    }

    @Test
    void shouldThrowExceptionWhenAuthenticationFails() throws Exception {
      final Map<String, String> challenge = HmacSha512Authenticator.newChallenge();
      final Map<String, String> response =
          HmacSha512Authenticator.newResponse("otherPassword", challenge);

      final Exception e =
          assertThrows(
              AuthenticationException.class,
              () -> HmacSha512Authenticator.authenticate(PASSWORD, challenge, response));

      assertThat(e.getMessage(), containsString("authentication failed"));
    }

    @Test
    void shouldThrowExceptionWhenChallengeDoesNotContainNonce() throws Exception {
      final Map<String, String> challenge = HmacSha512Authenticator.newChallenge();
      final Map<String, String> response = HmacSha512Authenticator.newResponse(PASSWORD, challenge);

      challenge.remove(ChallengePropertyNames.NONCE);

      final Exception e =
          assertThrows(
              AuthenticationException.class,
              () -> HmacSha512Authenticator.authenticate(PASSWORD, challenge, response));

      assertThat(
          e.getMessage(),
          allOf(containsString("missing"), containsString(ChallengePropertyNames.NONCE)));
    }

    @Test
    void shouldThrowExceptionWhenChallengeDoesNotContainSalt() throws Exception {
      final Map<String, String> challenge = HmacSha512Authenticator.newChallenge();
      final Map<String, String> response = HmacSha512Authenticator.newResponse(PASSWORD, challenge);

      challenge.remove(ChallengePropertyNames.SALT);

      final Exception e =
          assertThrows(
              AuthenticationException.class,
              () -> HmacSha512Authenticator.authenticate(PASSWORD, challenge, response));

      assertThat(
          e.getMessage(),
          allOf(containsString("missing"), containsString(ChallengePropertyNames.SALT)));
    }

    @Test
    void shouldThrowExceptionWhenResponseDoesNotContainDigest() throws Exception {
      final Map<String, String> challenge = HmacSha512Authenticator.newChallenge();
      final Map<String, String> response =
          new HashMap<>(HmacSha512Authenticator.newResponse(PASSWORD, challenge));

      response.remove(ResponsePropertyNames.DIGEST);

      final Exception e =
          assertThrows(
              AuthenticationException.class,
              () -> HmacSha512Authenticator.authenticate(PASSWORD, challenge, response));

      assertThat(
          e.getMessage(),
          allOf(containsString("missing"), containsString(ResponsePropertyNames.DIGEST)));
    }
  }

  @Nested
  final class DecodeRequiredPropertyTest {
    @Test
    void shouldThrowExceptionWhenNameAbsent() {
      final String name = "name";
      final Map<String, String> properties = ImmutableMap.of("other name", newBase64String());

      final Exception e =
          assertThrows(
              AuthenticationException.class,
              () -> HmacSha512Authenticator.decodeRequiredProperty(properties, name));
      assertThat(e.getMessage(), allOf(containsString("missing"), containsString(name)));
    }
  }
}
