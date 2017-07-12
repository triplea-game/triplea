package games.strategy.engine.framework.startup.login;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.net.SocketAddress;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;

import games.strategy.engine.framework.startup.login.ClientLoginValidator.ErrorMessages;
import games.strategy.net.IServerMessenger;

@RunWith(Enclosed.class)
public final class ClientLoginValidatorTests {
  private static final String PASSWORD = "password";
  private static final String OTHER_PASSWORD = "otherPassword";

  @RunWith(Enclosed.class)
  public static final class GetChallengePropertiesTests {
    @RunWith(MockitoJUnitRunner.StrictStubs.class)
    public abstract static class AbstractTestCase {
      @InjectMocks
      ClientLoginValidator clientLoginValidator;

      @Mock
      private IServerMessenger serverMessenger;

      @Mock
      private SocketAddress socketAddress;

      Map<String, String> getChallengeProperties() {
        return clientLoginValidator.getChallengeProperties("userName", socketAddress);
      }
    }

    public static final class WhenPasswordSetTest extends AbstractTestCase {
      @Before
      public void givenPasswordSet() {
        clientLoginValidator.setGamePassword(PASSWORD);
      }

      @Test
      public void shouldReturnChallengeProcessableByV1Authenticator() {
        final Map<String, String> challenge = getChallengeProperties();

        assertThat(V1Authenticator.canProcessChallenge(challenge), is(true));
      }

      @Test
      public void shouldReturnChallengeProcessableByV2Authenticator() {
        final Map<String, String> challenge = getChallengeProperties();

        assertThat(V2Authenticator.canProcessChallenge(challenge), is(true));
      }
    }

    public static final class WhenPasswordNotSetTest extends AbstractTestCase {
      @Before
      public void givenPasswordNotSet() {
        clientLoginValidator.setGamePassword(null);
      }

      @Test
      public void shouldReturnChallengeIgnoredByV1Authenticator() {
        final Map<String, String> challenge = getChallengeProperties();

        assertThat(V1Authenticator.canProcessChallenge(challenge), is(false));
      }

      @Test
      public void shouldReturnChallengeIgnoredByV2Authenticator() {
        final Map<String, String> challenge = getChallengeProperties();

        assertThat(V2Authenticator.canProcessChallenge(challenge), is(false));
      }
    }
  }

  @RunWith(MockitoJUnitRunner.StrictStubs.class)
  public static final class AuthenticateTest {
    @InjectMocks
    private ClientLoginValidator clientLoginValidator;

    @Mock
    private IServerMessenger serverMessenger;

    @Before
    public void givenPasswordSet() {
      clientLoginValidator.setGamePassword(PASSWORD);
    }

    @Test
    public void shouldReturnNoErrorWhenV1AuthenticationSucceeded() throws Exception {
      final Map<String, String> challenge = V1Authenticator.newChallenge();
      final Map<String, String> response = V1Authenticator.newResponse(PASSWORD, challenge);

      final String errorMessage = clientLoginValidator.authenticate(challenge, response);

      assertThat(errorMessage, is(nullValue()));
    }

    @Test
    public void shouldReturnErrorWhenV1AuthenticationFailed() throws Exception {
      final Map<String, String> challenge = V1Authenticator.newChallenge();
      final Map<String, String> response = V1Authenticator.newResponse(OTHER_PASSWORD, challenge);

      final String errorMessage = clientLoginValidator.authenticate(challenge, response);

      assertThat(errorMessage, is(ErrorMessages.INVALID_PASSWORD));
    }

    @Test
    public void shouldReturnNoErrorWhenV2AuthenticationSucceeded() throws Exception {
      final Map<String, String> challenge = V2Authenticator.newChallenge();
      final Map<String, String> response = V2Authenticator.newResponse(PASSWORD, challenge);

      final String errorMessage = clientLoginValidator.authenticate(challenge, response);

      assertThat(errorMessage, is(ErrorMessages.NO_ERROR));
    }

    @Test
    public void shouldReturnErrorWhenV2AuthenticationFailed() throws Exception {
      final Map<String, String> challenge = V2Authenticator.newChallenge();
      final Map<String, String> response = V2Authenticator.newResponse(OTHER_PASSWORD, challenge);

      final String errorMessage = clientLoginValidator.authenticate(challenge, response);

      assertThat(errorMessage, is(ErrorMessages.INVALID_PASSWORD));
    }

    @Test
    public void shouldBypassV1AuthenticationWhenV2AuthenticationSucceeded() throws Exception {
      final Map<String, String> challenge = ImmutableMap.<String, String>builder()
          .putAll(V1Authenticator.newChallenge())
          .putAll(V2Authenticator.newChallenge())
          .build();
      final Map<String, String> response = ImmutableMap.<String, String>builder()
          .putAll(V1Authenticator.newResponse(OTHER_PASSWORD, challenge))
          .putAll(V2Authenticator.newResponse(PASSWORD, challenge))
          .build();

      final String errorMessage = clientLoginValidator.authenticate(challenge, response);

      assertThat(errorMessage, is(ErrorMessages.NO_ERROR));
    }
  }
}
