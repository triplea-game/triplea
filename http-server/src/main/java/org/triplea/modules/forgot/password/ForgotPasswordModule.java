package org.triplea.modules.forgot.password;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.http.client.forgot.password.ForgotPasswordRequest;

/**
 * Module to orchestrate generating a temporary password for a user, storing that password in the
 * temp password table and sending that password in an email to the user.
 */
@Builder
public class ForgotPasswordModule implements BiFunction<String, ForgotPasswordRequest, String> {

  @VisibleForTesting
  static final String ERROR_TOO_MANY_REQUESTS = "Error, too many password reset attempts";

  @VisibleForTesting
  static final String ERROR_BAD_USER_OR_EMAIL =
      "Error, temp password not generated - check username and email";

  @VisibleForTesting
  static final String SUCCESS_MESSAGE = "A temporary password has been sent to your email.";

  @Nonnull private final BiConsumer<String, String> passwordEmailSender;
  @Nonnull private final PasswordGenerator passwordGenerator;
  @Nonnull private final TempPasswordPersistence tempPasswordPersistence;
  @Nonnull private final TempPasswordHistory tempPasswordHistory;

  @Override
  public String apply(final String inetAddress, final ForgotPasswordRequest forgotPasswordRequest) {
    checkArgs(inetAddress, forgotPasswordRequest);

    if (!tempPasswordHistory.allowRequestFromAddress(inetAddress)) {
      return ERROR_TOO_MANY_REQUESTS;
    }
    tempPasswordHistory.recordTempPasswordRequest(inetAddress, forgotPasswordRequest.getUsername());

    final String generatedPassword = passwordGenerator.generatePassword();
    if (!tempPasswordPersistence.storeTempPassword(forgotPasswordRequest, generatedPassword)) {
      return ERROR_BAD_USER_OR_EMAIL;
    }
    passwordEmailSender.accept(forgotPasswordRequest.getEmail(), generatedPassword);

    return SUCCESS_MESSAGE;
  }

  private static void checkArgs(
      final String inetAddress, final ForgotPasswordRequest forgotPasswordRequest) {
    checkArgument(!Strings.nullToEmpty(inetAddress).trim().isEmpty());
    try {
      //noinspection ResultOfMethodCallIgnored
      InetAddress.getAllByName(inetAddress);
    } catch (final UnknownHostException e) {
      throw new IllegalArgumentException("Invalid IP address: " + inetAddress);
    }
    checkArgument(!Strings.nullToEmpty(forgotPasswordRequest.getEmail()).trim().isEmpty());
    checkArgument(!Strings.nullToEmpty(forgotPasswordRequest.getUsername()).trim().isEmpty());
  }
}
