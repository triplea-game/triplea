package games.strategy.net.nio;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.net.TempPasswordHistory;
import java.net.InetAddress;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.jetbrains.annotations.NonNls;

/**
 * A subsequence of server conversation to handle requests for password reset. Returns a magic
 * string to indicate success or an error message.
 */
@Builder
public class ForgotPasswordConversation {

  @NonNls public static final String TEMP_PASSWORD_GENERATED_RESPONSE = "password_reset";
  @VisibleForTesting static final int MAX_TEMP_PASSWORD_REQUESTS_PER_DAY = 3;

  @Nonnull private final Predicate<String> forgotPasswordModule;
  @Nonnull private final TempPasswordHistory tempPasswordHistory;

  String handle(final InetAddress requestingAddress, final String username) {
    if (tempPasswordHistory.countRequestsFromAddress(requestingAddress)
        >= MAX_TEMP_PASSWORD_REQUESTS_PER_DAY) {
      return "Too many password reset attempts";
    }

    final String result =
        forgotPasswordModule.test(username)
            ? TEMP_PASSWORD_GENERATED_RESPONSE
            : "Error, temp password not generated - check username";
    if (result.equals(TEMP_PASSWORD_GENERATED_RESPONSE)) {
      tempPasswordHistory.recordTempPasswordRequest(requestingAddress, username);
    }
    return result;
  }
}
