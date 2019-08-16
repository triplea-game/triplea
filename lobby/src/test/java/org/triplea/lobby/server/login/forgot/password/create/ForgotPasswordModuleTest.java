package org.triplea.lobby.server.login.forgot.password.create;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordModuleTest {
  private static final String PASSWORD = "temp";
  private static final String USERNAME = "user";
  private static final String EMAIL = "email";

  @Mock private Function<String, Optional<String>> emailLookup;
  @Mock private PasswordEmailSender passwordEmailSender;
  @Mock private PasswordGenerator passwordGenerator;
  @Mock private TempPasswordPersistence tempPasswordPersistence;

  @InjectMocks private ForgotPasswordModule forgotPasswordModule;

  @Test
  void changePasswordUsernameNotFound() {
    when(emailLookup.apply(USERNAME)).thenReturn(Optional.of(EMAIL));
    when(passwordGenerator.generatePassword()).thenReturn(PASSWORD);
    when(tempPasswordPersistence.storeTempPassword(USERNAME, PASSWORD)).thenReturn(false);

    assertThat(forgotPasswordModule.test(USERNAME), is(false));

    verify(passwordEmailSender, never()).accept(any(), any());
  }

  @Test
  void userWithNoEmail() {
    when(emailLookup.apply(USERNAME)).thenReturn(Optional.empty());

    assertThat(forgotPasswordModule.test(USERNAME), is(false));

    verify(passwordGenerator, never()).generatePassword();
    verify(tempPasswordPersistence, never()).storeTempPassword(any(), any());
    verify(passwordEmailSender, never()).accept(any(), any());
  }

  @Test
  void changePasswordSuccess() {
    when(emailLookup.apply(USERNAME)).thenReturn(Optional.of(EMAIL));
    when(passwordGenerator.generatePassword()).thenReturn(PASSWORD);
    when(tempPasswordPersistence.storeTempPassword(USERNAME, PASSWORD)).thenReturn(true);

    assertThat(forgotPasswordModule.test(USERNAME), is(true));

    verify(passwordEmailSender).accept(EMAIL, PASSWORD);
  }
}
