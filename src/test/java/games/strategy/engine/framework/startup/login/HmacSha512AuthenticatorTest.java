package games.strategy.engine.framework.startup.login;

import static games.strategy.test.Assertions.assertNotThrows;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import games.strategy.engine.framework.startup.login.HmacSha512Authenticator.ChallengePropertyNames;
import games.strategy.engine.framework.startup.login.HmacSha512Authenticator.ResponsePropertyNames;

public final class HmacSha512AuthenticatorTest {
  private static final String PASSWORD = "←PASSWORD↑WITH→UNICODE↓CHARS";

  @Test
  public void canProcessChallenge_ShouldReturnTrueWhenAllPropertiesPresent() {
    final Map<String, String> challenge = newChallengeWithAllProperties();

    assertThat(HmacSha512Authenticator.canProcessChallenge(challenge), is(true));
  }

  private static Map<String, String> newChallengeWithAllProperties() {
    return Maps.newHashMap(ImmutableMap.of(
        ChallengePropertyNames.NONCE, newBase64String(),
        ChallengePropertyNames.SALT, newBase64String()));
  }

  private static String newBase64String() {
    final byte[] bytes = new byte[8];
    Arrays.fill(bytes, (byte) 1);
    return Base64.getEncoder().encodeToString(bytes);
  }

  @Test
  public void canProcessChallenge_ShouldReturnFalseWhenNonceAbsent() {
    final Map<String, String> challenge = newChallengeWithAllPropertiesExcept(ChallengePropertyNames.NONCE);

    assertThat(HmacSha512Authenticator.canProcessChallenge(challenge), is(false));
  }

  private static Map<String, String> newChallengeWithAllPropertiesExcept(final String name) {
    final Map<String, String> challenge = newChallengeWithAllProperties();
    challenge.remove(name);
    return challenge;
  }

  @Test
  public void canProcessChallenge_ShouldReturnFalseWhenSaltAbsent() {
    final Map<String, String> challenge = newChallengeWithAllPropertiesExcept(ChallengePropertyNames.SALT);

    assertThat(HmacSha512Authenticator.canProcessChallenge(challenge), is(false));
  }

  @Test
  public void newChallenge_ShouldIncludeNonceAndSalt() {
    final Map<String, String> challenge = HmacSha512Authenticator.newChallenge();

    assertThat(challenge, hasEntry(is(ChallengePropertyNames.NONCE), is(not(emptyString()))));
    assertThat(challenge, hasEntry(is(ChallengePropertyNames.SALT), is(not(emptyString()))));
  }

  @Test
  public void canProcessResponse_ShouldReturnTrueWhenAllPropertiesPresent() {
    final Map<String, String> response = newResponseWithAllProperties();

    assertThat(HmacSha512Authenticator.canProcessResponse(response), is(true));
  }

  private static Map<String, String> newResponseWithAllProperties() {
    return Maps.newHashMap(ImmutableMap.of(
        ResponsePropertyNames.DIGEST, newBase64String()));
  }

  @Test
  public void canProcessResponse_ShouldReturnFalseWhenDigestAbsent() {
    final Map<String, String> response = newResponseWithAllPropertiesExcept(ResponsePropertyNames.DIGEST);

    assertThat(HmacSha512Authenticator.canProcessResponse(response), is(false));
  }

  private static Map<String, String> newResponseWithAllPropertiesExcept(final String name) {
    final Map<String, String> response = newResponseWithAllProperties();
    response.remove(name);
    return response;
  }

  @Test
  public void newResponse_ShouldIncludeResponseWhenChallengeContainsNonceAndSalt() throws Exception {
    final Map<String, String> challenge = ImmutableMap.of(
        ChallengePropertyNames.NONCE, newBase64String(),
        ChallengePropertyNames.SALT, newBase64String());

    final Map<String, String> response = HmacSha512Authenticator.newResponse(PASSWORD, challenge);

    assertThat(response, hasEntry(is(ResponsePropertyNames.DIGEST), is(not(emptyString()))));
  }

  @Test
  public void newResponse_ShouldNotIncludeResponseWhenChallengeDoesNotContainNonce() throws Exception {
    final Map<String, String> challenge = ImmutableMap.of(ChallengePropertyNames.SALT, newBase64String());

    final Map<String, String> response = HmacSha512Authenticator.newResponse(PASSWORD, challenge);

    assertThat(response, not(hasKey(ResponsePropertyNames.DIGEST)));
  }

  @Test
  public void newResponse_ShouldNotIncludeResponseWhenChallengeDoesNotContainSalt() throws Exception {
    final Map<String, String> challenge = ImmutableMap.of(ChallengePropertyNames.NONCE, newBase64String());

    final Map<String, String> response = HmacSha512Authenticator.newResponse(PASSWORD, challenge);

    assertThat(response, not(hasKey(ResponsePropertyNames.DIGEST)));
  }

  @Test
  public void decodeOptionalProperty_ShouldReturnDecodedValueWhenNamePresent() throws Exception {
    final String name = "name";
    final String encodedValue = newBase64String();
    final Map<String, String> properties = ImmutableMap.of(name, encodedValue);

    final byte[] actualValue = HmacSha512Authenticator.decodeOptionalProperty(properties, name);

    assertThat(actualValue, is(Base64.getDecoder().decode(encodedValue)));
  }

  @Test
  public void decodeOptionalProperty_ShouldReturnNullWhenNameAbsent() throws Exception {
    final Map<String, String> properties = ImmutableMap.of("other name", newBase64String());

    final byte[] value = HmacSha512Authenticator.decodeOptionalProperty(properties, "name");

    assertThat(value, is(nullValue()));
  }

  @Test
  public void decodeOptionalProperty_ShouldThrowExceptionWhenValueMalformed() {
    final String name = "name";
    final Map<String, String> properties = ImmutableMap.of(name, "NOT_A_BASE64_VALUE");

    final Exception e = assertThrows(AuthenticationException.class,
        () -> HmacSha512Authenticator.decodeOptionalProperty(properties, name));
    assertThat(e.getMessage(), allOf(
        containsString("malformed"),
        containsString(name)));
  }

  @Test
  public void authenticate_ShouldNotThrowExceptionWhenAuthenticationSucceeds() throws Exception {
    final Map<String, String> challenge = HmacSha512Authenticator.newChallenge();
    final Map<String, String> response = HmacSha512Authenticator.newResponse(PASSWORD, challenge);

    assertNotThrows(() -> HmacSha512Authenticator.authenticate(PASSWORD, challenge, response));
  }

  @Test
  public void authenticate_ShouldThrowExceptionWhenAuthenticationFails() throws Exception {
    final Map<String, String> challenge = HmacSha512Authenticator.newChallenge();
    final Map<String, String> response = HmacSha512Authenticator.newResponse("otherPassword", challenge);

    final Exception e = assertThrows(AuthenticationException.class,
        () -> HmacSha512Authenticator.authenticate(PASSWORD, challenge, response));

    assertThat(e.getMessage(), containsString("authentication failed"));
  }

  @Test
  public void authenticate_ShouldThrowExceptionWhenChallengeDoesNotContainNonce() throws Exception {
    final Map<String, String> challenge = HmacSha512Authenticator.newChallenge();
    final Map<String, String> response = HmacSha512Authenticator.newResponse(PASSWORD, challenge);

    challenge.remove(ChallengePropertyNames.NONCE);

    final Exception e = assertThrows(AuthenticationException.class,
        () -> HmacSha512Authenticator.authenticate(PASSWORD, challenge, response));

    assertThat(e.getMessage(), allOf(
        containsString("missing"),
        containsString(ChallengePropertyNames.NONCE)));
  }

  @Test
  public void authenticate_ShouldThrowExceptionWhenChallengeDoesNotContainSalt() throws Exception {
    final Map<String, String> challenge = HmacSha512Authenticator.newChallenge();
    final Map<String, String> response = HmacSha512Authenticator.newResponse(PASSWORD, challenge);

    challenge.remove(ChallengePropertyNames.SALT);

    final Exception e = assertThrows(AuthenticationException.class,
        () -> HmacSha512Authenticator.authenticate(PASSWORD, challenge, response));

    assertThat(e.getMessage(), allOf(
        containsString("missing"),
        containsString(ChallengePropertyNames.SALT)));
  }

  @Test
  public void authenticate_ShouldThrowExceptionWhenResponseDoesNotContainDigest() throws Exception {
    final Map<String, String> challenge = HmacSha512Authenticator.newChallenge();
    final Map<String, String> response = HmacSha512Authenticator.newResponse(PASSWORD, challenge);

    response.remove(ResponsePropertyNames.DIGEST);

    final Exception e = assertThrows(AuthenticationException.class,
        () -> HmacSha512Authenticator.authenticate(PASSWORD, challenge, response));

    assertThat(e.getMessage(), allOf(
        containsString("missing"),
        containsString(ResponsePropertyNames.DIGEST)));
  }

  @Test
  public void decodeRequiredProperty_ShouldThrowExceptionWhenNameAbsent() {
    final String name = "name";
    final Map<String, String> properties = ImmutableMap.of("other name", newBase64String());

    final Exception e = assertThrows(AuthenticationException.class,
        () -> HmacSha512Authenticator.decodeRequiredProperty(properties, name));
    assertThat(e.getMessage(), allOf(
        containsString("missing"),
        containsString(name)));
  }
}
