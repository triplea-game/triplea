package org.triplea.modules.forgot.password;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.forgot.password.ForgotPasswordRequest;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordModuleTest {
  private static final String PASSWORD = "temp";
  private static final String IP_ADDRESS = "127.0.0.1";

  private static final ForgotPasswordRequest forgotPasswordRequest =
      ForgotPasswordRequest.builder().email("email").username("user").build();

  @Mock private PasswordEmailSender passwordEmailSender;
  @Mock private PasswordGenerator passwordGenerator;
  @Mock private TempPasswordPersistence tempPasswordPersistence;
  @Mock private TempPasswordHistory tempPasswordHistory;

  @InjectMocks private ForgotPasswordModule forgotPasswordModule;

  @Test
  void verifyArgValidation() {
    assertThrows(
        IllegalArgumentException.class,
        () -> forgotPasswordModule.apply(null, forgotPasswordRequest));
    assertThrows(
        IllegalArgumentException.class,
        () -> forgotPasswordModule.apply("", forgotPasswordRequest));
    assertThrows(
        IllegalArgumentException.class,
        () -> forgotPasswordModule.apply("not.an.ip.address", forgotPasswordRequest));
    assertThrows(
        IllegalArgumentException.class,
        () -> forgotPasswordModule.apply("127.0.0.1", ForgotPasswordRequest.builder().build()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            forgotPasswordModule.apply(
                "127.0.0.1",
                ForgotPasswordRequest.builder().username("  ").email("email@email.com").build()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            forgotPasswordModule.apply(
                "127.0.0.1",
                ForgotPasswordRequest.builder().username("username").email("   ").build()));
  }

  @Test
  void tooManyPasswordResetAttempts() {
    when(tempPasswordHistory.allowRequestFromAddress(IP_ADDRESS)).thenReturn(false);

    assertThat(
        forgotPasswordModule.apply(IP_ADDRESS, forgotPasswordRequest),
        is(ForgotPasswordModule.ERROR_TOO_MANY_REQUESTS));

    verify(tempPasswordHistory, never()).recordTempPasswordRequest(any(), any());
    verify(passwordGenerator, never()).generatePassword();
    verify(tempPasswordPersistence, never()).storeTempPassword(any(), any());
    verify(passwordEmailSender, never()).accept(any(), any());
  }

  @Test
  void badEmailOrUser() {
    when(tempPasswordHistory.allowRequestFromAddress(IP_ADDRESS)).thenReturn(true);
    when(passwordGenerator.generatePassword()).thenReturn(PASSWORD);
    when(tempPasswordPersistence.storeTempPassword(forgotPasswordRequest, PASSWORD))
        .thenReturn(false);

    assertThat(
        forgotPasswordModule.apply(IP_ADDRESS, forgotPasswordRequest),
        is(ForgotPasswordModule.ERROR_BAD_USER_OR_EMAIL));

    verify(passwordEmailSender, never()).accept(any(), any());
  }

  @Test
  void successCase() {
    when(tempPasswordHistory.allowRequestFromAddress(IP_ADDRESS)).thenReturn(true);
    when(passwordGenerator.generatePassword()).thenReturn(PASSWORD);
    when(tempPasswordPersistence.storeTempPassword(forgotPasswordRequest, PASSWORD))
        .thenReturn(true);

    assertThat(
        forgotPasswordModule.apply(IP_ADDRESS, forgotPasswordRequest),
        is(ForgotPasswordModule.SUCCESS_MESSAGE));

    verify(passwordEmailSender).accept(forgotPasswordRequest.getEmail(), PASSWORD);
  }
}
