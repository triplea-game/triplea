package games.strategy.engine.framework.startup.login;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import games.strategy.engine.framework.startup.login.ClientLoginValidator.ErrorMessages;


@RunWith(Enclosed.class)
public final class ClientLoginValidatorTests {
  private static final String PASSWORD = "password";
  private static final String OTHER_PASSWORD = "otherPassword";

  private static final String MAGIC_MAC_START = ClientLoginValidator.MAC_MAGIC_STRING_PREFIX;

  public static final class MacValidationTest {
    @Test
    public void invalidMacs() {
      Arrays.asList(
              MAGIC_MAC_START + "no spaces allowed",
              MAGIC_MAC_START + "tooShort",
              MAGIC_MAC_START + "#%@symbol",
              Strings.repeat("0", ClientLoginValidator.MAC_ADDRESS_LENGTH)
      ).forEach(invalidValue -> assertThat(ClientLoginValidator.isValidMac(invalidValue), is(false)));
    }

    @Test
    public void validMac() {
      int remainingPaddingLength = ClientLoginValidator.MAC_ADDRESS_LENGTH - MAGIC_MAC_START.length();
      String valid = MAGIC_MAC_START + Strings.repeat("0", remainingPaddingLength);

      assertThat(ClientLoginValidator.isValidMac(valid), is(true));
    }
  }

  @RunWith(Enclosed.class)
  public static final class GetChallengePropertiesTests {
    public abstract static class AbstractTestCase {
      @InjectMocks
      ClientLoginValidator clientLoginValidator;

      @Mock
      private SocketAddress socketAddress;

      Map<String, String> getChallengeProperties() {
        return clientLoginValidator.getChallengeProperties("userName", socketAddress);
      }
    }

    @RunWith(MockitoJUnitRunner.StrictStubs.class)
    public static final class WhenPasswordSetTest extends AbstractTestCase {
      @Before
      public void givenPasswordSet() {
        clientLoginValidator.setGamePassword(PASSWORD);
      }

      @Test
      public void shouldReturnChallengeProcessableByMd5CryptAuthenticator() {
        final Map<String, String> challenge = getChallengeProperties();

        assertThat(Md5CryptAuthenticator.canProcessChallenge(challenge), is(true));
      }

      @Test
      public void shouldReturnChallengeProcessableByHmacSha512Authenticator() {
        final Map<String, String> challenge = getChallengeProperties();

        assertThat(HmacSha512Authenticator.canProcessChallenge(challenge), is(true));
      }
    }

    @RunWith(MockitoJUnitRunner.StrictStubs.class)
    public static final class WhenPasswordNotSetTest extends AbstractTestCase {
      @Before
      public void givenPasswordNotSet() {
        clientLoginValidator.setGamePassword(null);
      }

      @Test
      public void shouldReturnChallengeIgnoredByMd5CryptAuthenticator() {
        final Map<String, String> challenge = getChallengeProperties();

        assertThat(Md5CryptAuthenticator.canProcessChallenge(challenge), is(false));
      }

      @Test
      public void shouldReturnChallengeIgnoredByHmacSha512Authenticator() {
        final Map<String, String> challenge = getChallengeProperties();

        assertThat(HmacSha512Authenticator.canProcessChallenge(challenge), is(false));
      }
    }
  }

  @RunWith(MockitoJUnitRunner.StrictStubs.class)
  public static final class AuthenticateTest {
    @InjectMocks
    private ClientLoginValidator clientLoginValidator;

    @Before
    public void givenPasswordSet() {
      clientLoginValidator.setGamePassword(PASSWORD);
    }

    @Test
    public void shouldReturnNoErrorWhenMd5CryptAuthenticationSucceeded() throws Exception {
      final Map<String, String> challenge = Md5CryptAuthenticator.newChallenge();
      final Map<String, String> response = Md5CryptAuthenticator.newResponse(PASSWORD, challenge);

      final String errorMessage = clientLoginValidator.authenticate(challenge, response);

      assertThat(errorMessage, is(nullValue()));
    }

    @Test
    public void shouldReturnErrorWhenMd5CryptAuthenticationFailed() throws Exception {
      final Map<String, String> challenge = Md5CryptAuthenticator.newChallenge();
      final Map<String, String> response = Md5CryptAuthenticator.newResponse(OTHER_PASSWORD, challenge);

      final String errorMessage = clientLoginValidator.authenticate(challenge, response);

      assertThat(errorMessage, is(ErrorMessages.INVALID_PASSWORD));
    }

    @Test
    public void shouldReturnNoErrorWhenHmacSha512AuthenticationSucceeded() throws Exception {
      final Map<String, String> challenge = HmacSha512Authenticator.newChallenge();
      final Map<String, String> response = HmacSha512Authenticator.newResponse(PASSWORD, challenge);

      final String errorMessage = clientLoginValidator.authenticate(challenge, response);

      assertThat(errorMessage, is(ErrorMessages.NO_ERROR));
    }

    @Test
    public void shouldReturnErrorWhenHmacSha512AuthenticationFailed() throws Exception {
      final Map<String, String> challenge = HmacSha512Authenticator.newChallenge();
      final Map<String, String> response = HmacSha512Authenticator.newResponse(OTHER_PASSWORD, challenge);

      final String errorMessage = clientLoginValidator.authenticate(challenge, response);

      assertThat(errorMessage, is(ErrorMessages.INVALID_PASSWORD));
    }

    @Test
    public void shouldBypassMd5CryptAuthenticationWhenHmacSha512AuthenticationSucceeded() throws Exception {
      final Map<String, String> challenge = ImmutableMap.<String, String>builder()
          .putAll(Md5CryptAuthenticator.newChallenge())
          .putAll(HmacSha512Authenticator.newChallenge())
          .build();
      final Map<String, String> response = ImmutableMap.<String, String>builder()
          .putAll(Md5CryptAuthenticator.newResponse(OTHER_PASSWORD, challenge))
          .putAll(HmacSha512Authenticator.newResponse(PASSWORD, challenge))
          .build();

      final String errorMessage = clientLoginValidator.authenticate(challenge, response);

      assertThat(errorMessage, is(ErrorMessages.NO_ERROR));
    }
  }
}
