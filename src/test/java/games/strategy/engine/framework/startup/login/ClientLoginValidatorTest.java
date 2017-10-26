package games.strategy.engine.framework.startup.login;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.experimental.extensions.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import games.strategy.engine.framework.startup.login.ClientLoginValidator.ErrorMessages;

public final class ClientLoginValidatorTest {
  private static final String PASSWORD = "password";
  private static final String OTHER_PASSWORD = "otherPassword";

  @Nested
  public final class MacValidationTest {
    private static final String MAGIC_MAC_START = ClientLoginValidator.MAC_MAGIC_STRING_PREFIX;

    @Test
    public void invalidMacs() {
      final Collection<String> macs = Arrays.asList(
          MAGIC_MAC_START + "no spaces allowed",
          MAGIC_MAC_START + "tooShort",
          MAGIC_MAC_START + "#%@symbol",
          Strings.repeat("0", ClientLoginValidator.MAC_ADDRESS_LENGTH));
      macs.forEach(invalidValue -> assertThat(ClientLoginValidator.isValidMac(invalidValue), is(false)));
    }

    @Test
    public void validMac() {
      final int remainingPaddingLength = ClientLoginValidator.MAC_ADDRESS_LENGTH - MAGIC_MAC_START.length();
      final String valid = MAGIC_MAC_START + Strings.repeat("0", remainingPaddingLength);

      assertThat(ClientLoginValidator.isValidMac(valid), is(true));
    }
  }

  @Nested
  public final class GetChallengePropertiesTest {
    public abstract class AbstractTestCase {
      final ClientLoginValidator clientLoginValidator = new ClientLoginValidator(null);

      @Mock
      private SocketAddress socketAddress;

      Map<String, String> getChallengeProperties() {
        return clientLoginValidator.getChallengeProperties("userName", socketAddress);
      }
    }

    @ExtendWith(MockitoExtension.class)
    @Nested
    public final class WhenPasswordSetTest extends AbstractTestCase {
      @BeforeEach
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

    @ExtendWith(MockitoExtension.class)
    @Nested
    public final class WhenPasswordNotSetTest extends AbstractTestCase {
      @BeforeEach
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

  @ExtendWith(MockitoExtension.class)
  @Nested
  public final class AuthenticateTest {
    private final ClientLoginValidator clientLoginValidator = new ClientLoginValidator(null);

    @BeforeEach
    public void givenPasswordSet() {
      clientLoginValidator.setGamePassword(PASSWORD);
    }

    @Test
    public void shouldReturnNoErrorWhenMd5CryptAuthenticationSucceeded() throws Exception {
      final Map<String, String> challenge = Md5CryptAuthenticator.newChallenge();
      final Map<String, String> response = Md5CryptAuthenticator.newResponse(PASSWORD, challenge);

      final String errorMessage = clientLoginValidator.authenticate(challenge, response);

      assertThat(errorMessage, is(ErrorMessages.NO_ERROR));
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
