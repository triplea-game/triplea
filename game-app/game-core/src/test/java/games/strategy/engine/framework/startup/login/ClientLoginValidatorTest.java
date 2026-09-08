package games.strategy.engine.framework.startup.login;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.framework.startup.login.ClientLoginValidator.ErrorMessages;
import java.util.Map;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

final class ClientLoginValidatorTest {
  @NonNls private static final String PASSWORD = "password";
  @NonNls private static final String OTHER_PASSWORD = "otherPassword";

  @ExtendWith(MockitoExtension.class)
  @Nested
  final class AuthenticateTest {
    @Test
    void shouldReturnNoErrorWhenHmacSha512AuthenticationSucceeded() throws Exception {
      final Map<String, String> challenge = HmacSha512Authenticator.newChallenge();
      final Map<String, String> response = HmacSha512Authenticator.newResponse(PASSWORD, challenge);

      final String errorMessage = ClientLoginValidator.authenticate(challenge, response, PASSWORD);

      assertThat(errorMessage).isEqualTo(ErrorMessages.NO_ERROR);
    }

    @Test
    void shouldReturnErrorWhenHmacSha512AuthenticationFailed() throws Exception {
      final Map<String, String> challenge = HmacSha512Authenticator.newChallenge();
      final Map<String, String> response =
          HmacSha512Authenticator.newResponse(OTHER_PASSWORD, challenge);

      final String errorMessage = ClientLoginValidator.authenticate(challenge, response, PASSWORD);

      assertThat(errorMessage).isEqualTo(ErrorMessages.INVALID_PASSWORD);
    }
  }
}
