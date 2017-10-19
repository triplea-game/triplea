package games.strategy.engine.framework.startup.login;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static com.googlecode.catchexception.apis.CatchExceptionHamcrestMatchers.hasMessageThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsMapContaining.hasEntry;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import games.strategy.engine.framework.startup.login.Md5CryptAuthenticator.ChallengePropertyNames;
import games.strategy.engine.framework.startup.login.Md5CryptAuthenticator.ResponsePropertyNames;
import games.strategy.util.MD5Crypt;

public final class Md5CryptAuthenticatorTest {
  private static final String PASSWORD = "←PASSWORD↑WITH→UNICODE↓CHARS";

  @Test
  public void canProcessChallenge_ShouldReturnTrueWhenAllPropertiesPresent() {
    final Map<String, String> challenge = newChallengeWithAllProperties();

    assertThat(Md5CryptAuthenticator.canProcessChallenge(challenge), is(true));
  }

  private static Map<String, String> newChallengeWithAllProperties() {
    return Maps.newHashMap(ImmutableMap.of(
        ChallengePropertyNames.SALT, ""));
  }

  @Test
  public void canProcessChallenge_ShouldReturnFalseWhenSaltAbsent() {
    final Map<String, String> challenge = newChallengeWithAllPropertiesExcept(ChallengePropertyNames.SALT);

    assertThat(Md5CryptAuthenticator.canProcessChallenge(challenge), is(false));
  }

  private static Map<String, String> newChallengeWithAllPropertiesExcept(final String name) {
    final Map<String, String> challenge = newChallengeWithAllProperties();
    challenge.remove(name);
    return challenge;
  }

  @Test
  public void newChallenge_ShouldIncludeSalt() {
    final Map<String, String> challenge = Md5CryptAuthenticator.newChallenge();

    assertThat(challenge, hasEntry(is(ChallengePropertyNames.SALT), is(not(emptyString()))));
  }

  @Test
  public void canProcessResponse_ShouldReturnTrueWhenAllPropertiesPresent() {
    final Map<String, String> response = newResponseWithAllProperties();

    assertThat(Md5CryptAuthenticator.canProcessResponse(response), is(true));
  }

  private static Map<String, String> newResponseWithAllProperties() {
    return Maps.newHashMap(ImmutableMap.of(
        ResponsePropertyNames.DIGEST, ""));
  }

  @Test
  public void canProcessResponse_ShouldReturnFalseWhenDigestAbsent() {
    final Map<String, String> response = newResponseWithAllPropertiesExcept(ResponsePropertyNames.DIGEST);

    assertThat(Md5CryptAuthenticator.canProcessResponse(response), is(false));
  }

  private static Map<String, String> newResponseWithAllPropertiesExcept(final String name) {
    final Map<String, String> response = newResponseWithAllProperties();
    response.remove(name);
    return response;
  }

  @Test
  public void newResponse_ShouldIncludeResponseWhenChallengeContainsSalt() throws Exception {
    final Map<String, String> challenge = ImmutableMap.of(
        ChallengePropertyNames.SALT, MD5Crypt.newSalt());

    final Map<String, String> response = Md5CryptAuthenticator.newResponse(PASSWORD, challenge);

    assertThat(response, hasEntry(is(ResponsePropertyNames.DIGEST), is(not(emptyString()))));
  }

  @Test
  public void newResponse_ShouldThrowExceptionWhenChallengeDoesNotContainSalt() throws Exception {
    final Map<String, String> challenge = ImmutableMap.of();

    catchException(() -> Md5CryptAuthenticator.newResponse(PASSWORD, challenge));

    assertThat(caughtException(), allOf(
        is(instanceOf(AuthenticationException.class)),
        hasMessageThat(containsString("missing"))));
  }

  @Test
  public void getRequiredProperty_ShouldReturnValueWhenNamePresent() throws Exception {
    final String name = "name";
    final String value = "value";
    final Map<String, String> properties = ImmutableMap.of(name, value);

    final String actualValue = Md5CryptAuthenticator.getRequiredProperty(properties, name);

    assertThat(actualValue, is(value));
  }

  @Test
  public void getRequiredProperty_ShouldThrowExceptionWhenNameAbsent() {
    final String name = "name";
    final Map<String, String> properties = ImmutableMap.of("other name", "value");

    catchException(() -> Md5CryptAuthenticator.getRequiredProperty(properties, name));

    assertThat(caughtException(), allOf(
        is(instanceOf(AuthenticationException.class)),
        hasMessageThat(containsString("missing")),
        hasMessageThat(containsString(name))));
  }

  @Test
  public void authenticate_ShouldNotThrowExceptionWhenAuthenticationSucceeds() throws Exception {
    final Map<String, String> challenge = Md5CryptAuthenticator.newChallenge();
    final Map<String, String> response = Md5CryptAuthenticator.newResponse(PASSWORD, challenge);

    catchException(() -> Md5CryptAuthenticator.authenticate(PASSWORD, challenge, response));

    assertThat(caughtException(), is(nullValue()));
  }

  @Test
  public void authenticate_ShouldThrowExceptionWhenAuthenticationFails() throws Exception {
    final Map<String, String> challenge = Md5CryptAuthenticator.newChallenge();
    final Map<String, String> response = Md5CryptAuthenticator.newResponse("otherPassword", challenge);

    catchException(() -> Md5CryptAuthenticator.authenticate(PASSWORD, challenge, response));

    assertThat(caughtException(), allOf(
        is(instanceOf(AuthenticationException.class)),
        hasMessageThat(containsString("authentication failed"))));
  }

  @Test
  public void authenticate_ShouldThrowExceptionWhenChallengeDoesNotContainSalt() throws Exception {
    final Map<String, String> challenge = Md5CryptAuthenticator.newChallenge();
    final Map<String, String> response = Md5CryptAuthenticator.newResponse(PASSWORD, challenge);

    challenge.remove(ChallengePropertyNames.SALT);
    catchException(() -> Md5CryptAuthenticator.authenticate(PASSWORD, challenge, response));

    assertThat(caughtException(), allOf(
        is(instanceOf(AuthenticationException.class)),
        hasMessageThat(containsString("missing")),
        hasMessageThat(containsString(ChallengePropertyNames.SALT))));
  }

  @Test
  public void authenticate_ShouldThrowExceptionWhenResponseDoesNotContainDigest() throws Exception {
    final Map<String, String> challenge = Md5CryptAuthenticator.newChallenge();
    final Map<String, String> response = Md5CryptAuthenticator.newResponse(PASSWORD, challenge);

    response.remove(ResponsePropertyNames.DIGEST);
    catchException(() -> Md5CryptAuthenticator.authenticate(PASSWORD, challenge, response));

    assertThat(caughtException(), allOf(
        is(instanceOf(AuthenticationException.class)),
        hasMessageThat(containsString("missing")),
        hasMessageThat(containsString(ResponsePropertyNames.DIGEST))));
  }
}
