package games.strategy.engine.framework.startup.login;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static com.googlecode.catchexception.apis.CatchExceptionHamcrestMatchers.hasMessageThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import games.strategy.engine.framework.startup.login.V1Authenticator.ChallengePropertyNames;
import games.strategy.engine.framework.startup.login.V1Authenticator.ResponsePropertyNames;
import games.strategy.util.MD5Crypt;

public final class V1AuthenticatorTest {
  private static final String PASSWORD = "←PASSWORD↑WITH→UNICODE↓CHARS";

  @Test
  public void canProcessChallenge_ShouldReturnTrueWhenAllPropertiesPresent() {
    final Map<String, String> challenge = newChallengeWithAllProperties();

    assertThat(V1Authenticator.canProcessChallenge(challenge), is(true));
  }

  private static Map<String, String> newChallengeWithAllProperties() {
    return Maps.newHashMap(ImmutableMap.of(
        ChallengePropertyNames.SALT, ""));
  }

  @Test
  public void canProcessChallenge_ShouldReturnFalseWhenSaltAbsent() {
    final Map<String, String> challenge = newChallengeWithAllPropertiesExcept(ChallengePropertyNames.SALT);

    assertThat(V1Authenticator.canProcessChallenge(challenge), is(false));
  }

  private static Map<String, String> newChallengeWithAllPropertiesExcept(final String name) {
    final Map<String, String> challenge = newChallengeWithAllProperties();
    challenge.remove(name);
    return challenge;
  }

  @Test
  public void newChallenge_ShouldIncludeSalt() {
    final Map<String, String> challenge = V1Authenticator.newChallenge();

    assertThat(challenge, hasEntry(is(ChallengePropertyNames.SALT), is(not(emptyString()))));
  }

  @Test
  public void canProcessResponse_ShouldReturnTrueWhenAllPropertiesPresent() {
    final Map<String, String> response = newResponseWithAllProperties();

    assertThat(V1Authenticator.canProcessResponse(response), is(true));
  }

  private static Map<String, String> newResponseWithAllProperties() {
    return Maps.newHashMap(ImmutableMap.of(
        ResponsePropertyNames.DIGEST, ""));
  }

  @Test
  public void canProcessResponse_ShouldReturnFalseWhenDigestAbsent() {
    final Map<String, String> response = newResponseWithAllPropertiesExcept(ResponsePropertyNames.DIGEST);

    assertThat(V1Authenticator.canProcessResponse(response), is(false));
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

    final Map<String, String> response = V1Authenticator.newResponse(PASSWORD, challenge);

    assertThat(response, hasEntry(is(ResponsePropertyNames.DIGEST), is(not(emptyString()))));
  }

  @Test
  public void newResponse_ShouldThrowExceptionWhenChallengeDoesNotContainSalt() throws Exception {
    final Map<String, String> challenge = ImmutableMap.of();

    catchException(() -> V1Authenticator.newResponse(PASSWORD, challenge));

    assertThat(caughtException(), allOf(
        is(instanceOf(AuthenticationException.class)),
        hasMessageThat(containsString("missing"))));
  }

  @Test
  public void getRequiredProperty_ShouldReturnValueWhenNamePresent() throws Exception {
    final String name = "name";
    final String value = "value";
    final Map<String, String> properties = ImmutableMap.of(name, value);

    final String actualValue = V1Authenticator.getRequiredProperty(properties, name);

    assertThat(actualValue, is(value));
  }

  @Test
  public void getRequiredProperty_ShouldThrowExceptionWhenNameAbsent() {
    final String name = "name";
    final Map<String, String> properties = ImmutableMap.of("other name", "value");

    catchException(() -> V1Authenticator.getRequiredProperty(properties, name));

    assertThat(caughtException(), allOf(
        is(instanceOf(AuthenticationException.class)),
        hasMessageThat(containsString("missing")),
        hasMessageThat(containsString(name))));
  }

  @Test
  public void authenticate_ShouldNotThrowExceptionWhenAuthenticationSucceeds() throws Exception {
    final Map<String, String> challenge = V1Authenticator.newChallenge();
    final Map<String, String> response = V1Authenticator.newResponse(PASSWORD, challenge);

    catchException(() -> V1Authenticator.authenticate(PASSWORD, challenge, response));

    assertThat(caughtException(), is(nullValue()));
  }

  @Test
  public void authenticate_ShouldThrowExceptionWhenAuthenticationFails() throws Exception {
    final Map<String, String> challenge = V1Authenticator.newChallenge();
    final Map<String, String> response = V1Authenticator.newResponse("otherPassword", challenge);

    catchException(() -> V1Authenticator.authenticate(PASSWORD, challenge, response));

    assertThat(caughtException(), allOf(
        is(instanceOf(AuthenticationException.class)),
        hasMessageThat(containsString("authentication failed"))));
  }

  @Test
  public void authenticate_ShouldThrowExceptionWhenChallengeDoesNotContainSalt() throws Exception {
    final Map<String, String> challenge = V1Authenticator.newChallenge();
    final Map<String, String> response = V1Authenticator.newResponse(PASSWORD, challenge);

    challenge.remove(ChallengePropertyNames.SALT);
    catchException(() -> V1Authenticator.authenticate(PASSWORD, challenge, response));

    assertThat(caughtException(), allOf(
        is(instanceOf(AuthenticationException.class)),
        hasMessageThat(containsString("missing")),
        hasMessageThat(containsString(ChallengePropertyNames.SALT))));
  }

  @Test
  public void authenticate_ShouldThrowExceptionWhenResponseDoesNotContainDigest() throws Exception {
    final Map<String, String> challenge = V1Authenticator.newChallenge();
    final Map<String, String> response = V1Authenticator.newResponse(PASSWORD, challenge);

    response.remove(ResponsePropertyNames.DIGEST);
    catchException(() -> V1Authenticator.authenticate(PASSWORD, challenge, response));

    assertThat(caughtException(), allOf(
        is(instanceOf(AuthenticationException.class)),
        hasMessageThat(containsString("missing")),
        hasMessageThat(containsString(ResponsePropertyNames.DIGEST))));
  }
}
