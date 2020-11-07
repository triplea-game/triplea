package games.strategy.engine.framework.startup.login;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.framework.startup.login.ClientLoginValidator.ErrorMessages;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.util.Version;

final class ClientLoginValidatorTest {
  private static final String PASSWORD = "password";
  private static final String OTHER_PASSWORD = "otherPassword";

  @ExtendWith(MockitoExtension.class)
  @Nested
  final class AuthenticateTest {
    private final ClientLoginValidator clientLoginValidator =
        new ClientLoginValidator(new Version(2, 0, 0));

    @BeforeEach
    void givenPasswordSet() {
      clientLoginValidator.setGamePassword(PASSWORD);
    }

    @Test
    void shouldReturnNoErrorWhenHmacSha512AuthenticationSucceeded() throws Exception {
      final Map<String, String> challenge = HmacSha512Authenticator.newChallenge();
      final Map<String, String> response = HmacSha512Authenticator.newResponse(PASSWORD, challenge);

      final String errorMessage = clientLoginValidator.authenticate(challenge, response);

      assertThat(errorMessage, is(ErrorMessages.NO_ERROR));
    }

    @Test
    void shouldReturnErrorWhenHmacSha512AuthenticationFailed() throws Exception {
      final Map<String, String> challenge = HmacSha512Authenticator.newChallenge();
      final Map<String, String> response =
          HmacSha512Authenticator.newResponse(OTHER_PASSWORD, challenge);

      final String errorMessage = clientLoginValidator.authenticate(challenge, response);

      assertThat(errorMessage, is(ErrorMessages.INVALID_PASSWORD));
    }
  }
}
