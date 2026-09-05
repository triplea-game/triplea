package games.strategy.engine.framework.startup.ui.posted.game.pbem;

import static games.strategy.engine.framework.startup.ui.posted.game.pbem.EmailProviderPreset.GMAIL;
import static games.strategy.engine.framework.startup.ui.posted.game.pbem.EmailSenderEditorViewModel.GENERIC_SMTP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.posted.game.pbem.IEmailSender;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.settings.ClientSetting;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.java.ViewModelListener;

@ExtendWith(MockitoExtension.class)
class EmailSenderEditorViewModelTest extends AbstractClientSettingTestCase {

  @Mock private SendTestEmailAction sendTestEmailAction;
  @Mock private ViewModelListener<EmailSenderEditorViewModel> view;

  @Test
  void sendTestEmailAction() {
    final var viewModel = new EmailSenderEditorViewModel(view);
    viewModel.setSendTestEmailAction(sendTestEmailAction);
    viewModel.setToAddress("  toAddress  ");

    viewModel.sendTestEmail();

    // values should be trimmed!
    verify(sendTestEmailAction).send("toAddress");
  }

  @Nested
  class SelectedProvider {
    @Test
    void selectedProviderDefaultsToDisabled() {
      final var viewModel = new EmailSenderEditorViewModel(view);

      assertThat(viewModel.getSelectedProvider())
          .as("no client setting is set, default should be disabled")
          .isEqualTo(EmailSenderEditorViewModel.PROVIDER_DISABLED);
    }

    @Test
    void emailProviderDefaultsToClientSettingWhenSet() {
      ClientSetting.emailProvider.setValueAndFlush("email provider");
      final var viewModel = new EmailSenderEditorViewModel(view);

      assertThat(viewModel.getSelectedProvider())
          .as("Value should match the client setting we have set")
          .isEqualTo("email provider");
    }

    @Test
    void updatingSelectedProviderFillsInSettings() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setSelectedProvider(GMAIL.getName());

      assertThat(viewModel.getSmtpServer()).isEqualTo(GMAIL.getServer());
      assertThat(viewModel.getSmtpPort()).isEqualTo(String.valueOf(GMAIL.getPort()));
      assertThat(viewModel.isUseTls()).isEqualTo(GMAIL.isUseTlsByDefault());
    }
  }

  @Nested
  class DisabledSmtpProviderSetting {
    @Test
    void disabledProviderFieldAreBlank() {
      final var viewModel = new EmailSenderEditorViewModel(view);

      viewModel.setSelectedProvider(EmailSenderEditorViewModel.PROVIDER_DISABLED);

      assertThat(viewModel.getSmtpServer()).isEmpty();
      assertThat(viewModel.getSmtpPort()).isEmpty();
      assertThat(viewModel.getEmailUsername()).isEmpty();
      assertThat(viewModel.getEmailPassword()).isEmpty();
    }

    @Test
    void disabledProviderDisablesServerAndEmailOptions() {
      final var viewModel = new EmailSenderEditorViewModel(view);

      viewModel.setSelectedProvider(EmailSenderEditorViewModel.PROVIDER_DISABLED);

      assertThat(viewModel.showServerOptions()).isFalse();
      assertThat(viewModel.showEmailOptions()).isFalse();
    }
  }

  @Nested
  @DisplayName("Verify setting a validated field to a new value invokes callbacks")
  class UpdatingValidatedFieldsInvokesCallbacks {
    @Mock private Runnable callback;

    @Test
    void setSelectedProviderInvokesViewCallback() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setValidatedFieldsChangedListener(callback);

      viewModel.setSelectedProvider("provider");
      viewModel.setSelectedProvider("provider");

      verify(view).viewModelChanged(viewModel);
      verify(callback).run();
    }

    @Test
    void setSmtpServerInvokesViewCallback() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setValidatedFieldsChangedListener(callback);

      viewModel.setSmtpServer("value");
      viewModel.setSmtpServer("value");

      verify(view).viewModelChanged(viewModel);
      verify(callback).run();
    }

    @Test
    void setSmtpPortInvokesViewCallback() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setValidatedFieldsChangedListener(callback);

      viewModel.setSmtpPort("port");
      viewModel.setSmtpPort("port");

      verify(view).viewModelChanged(viewModel);
      verify(callback).run();
    }

    @Test
    void setToAddressInvokesViewCallback() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setValidatedFieldsChangedListener(callback);

      viewModel.setToAddress("toAddress");
      viewModel.setToAddress("toAddress");

      verify(view).viewModelChanged(viewModel);
      verify(callback).run();
    }

    @Test
    void setSubjectInvokesViewCallback() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setValidatedFieldsChangedListener(callback);

      viewModel.setSubject("subject");
      viewModel.setSubject("subject");

      verify(view).viewModelChanged(viewModel);
      verify(callback).run();
    }

    @Test
    void setUsernameInvokesViewCallback() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setValidatedFieldsChangedListener(callback);

      viewModel.setEmailUsername("user-name");
      viewModel.setEmailUsername("user-name");

      verify(view).viewModelChanged(viewModel);
      verify(callback).run();
    }

    @Test
    void setPasswordInvokesViewCallback() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setValidatedFieldsChangedListener(callback);

      viewModel.setEmailPassword(new char[0]);

      verify(view).viewModelChanged(viewModel);
      verify(callback).run();
    }
  }

  @Nested
  @DisplayName("Verify conditions when send test email button is enabled")
  class TestEmailButtonEnablement {
    @Test
    void sendTestEmailIsDefaultDisabled() {
      final var viewModel = new EmailSenderEditorViewModel(view);

      assertThat(viewModel.isTestEmailButtonEnabled())
          .as(
              "By default we should not have smtp server settings or any others"
                  + "set, sending test email should be disabled")
          .isFalse();
    }

    @Test
    void sendTestEmailButtonEnabledWhenFieldsAreSet() {
      final var viewModel = givenViewModelWithEnabledSendTestEmailButton();

      assertThat(viewModel.isTestEmailButtonEnabled()).isTrue();
    }

    private EmailSenderEditorViewModel givenViewModelWithEnabledSendTestEmailButton() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setSmtpServer("server");
      viewModel.setSmtpPort("22");
      viewModel.setToAddress("toAddress@valid.com");
      viewModel.setEmailUsername("username");
      viewModel.setEmailPassword("password".toCharArray());
      return viewModel;
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not a nubmer", "nAn", "-1", "0"})
    void portMustBePositiveAndSet(final String invalidPortNumber) {
      final var viewModel = givenViewModelWithEnabledSendTestEmailButton();

      viewModel.setSmtpPort(invalidPortNumber);

      assertThat(viewModel.isTestEmailButtonEnabled())
          .as("Port number field is no longer valid")
          .isFalse();
    }

    @Test
    void smtpServerMustBeSet() {
      final var viewModel = givenViewModelWithEnabledSendTestEmailButton();

      viewModel.setSmtpServer("");

      assertThat(viewModel.isTestEmailButtonEnabled())
          .as("Smtp server is no longer valid")
          .isFalse();
    }

    @Test
    void smtpServerUsernameMustBeSet() {
      final var viewModel = givenViewModelWithEnabledSendTestEmailButton();

      viewModel.setEmailUsername("");

      assertThat(viewModel.isTestEmailButtonEnabled()).isFalse();
    }

    @Test
    void smtpServerPasswordMustBeSet() {
      final var viewModel = givenViewModelWithEnabledSendTestEmailButton();

      viewModel.setEmailPassword(new char[0]);

      assertThat(viewModel.isTestEmailButtonEnabled()).isFalse();
    }

    @Test
    void toAddressMustBeValidEmail() {
      final var viewModel = givenViewModelWithEnabledSendTestEmailButton();

      viewModel.setToAddress("not-valid");

      assertThat(viewModel.isTestEmailButtonEnabled()).isFalse();
    }
  }

  @Nested
  @DisplayName("Verify default values are pinned to client setting values")
  class ClientSettingDefaultValues {

    @Test
    void defaultValuesAreBlankWithoutAnyClientSettings() {
      final var viewModel = new EmailSenderEditorViewModel(view);

      assertThat(viewModel.getSmtpServer()).isEmpty();
      assertThat(viewModel.getSmtpPort()).isEmpty();
      assertThat(viewModel.isUseTls()).isTrue();
    }

    @Test
    void smtpServerDefaultsToClientSetting() {
      ClientSetting.emailServerHost.setValueAndFlush("server");
      final var viewModel = new EmailSenderEditorViewModel(view);

      assertThat(viewModel.getSmtpServer()).isEqualTo("server");
    }

    @Test
    void smtpPortDefaultsToClientSetting() {
      ClientSetting.emailServerPort.setValueAndFlush(500);
      final var viewModel = new EmailSenderEditorViewModel(view);

      assertThat(viewModel.getSmtpPort()).isEqualTo("500");
    }

    @Test
    void useTlsDefaultsToClientSetting() {
      ClientSetting.emailServerSecurity.setValueAndFlush(false);
      final var viewModel = new EmailSenderEditorViewModel(view);

      assertThat(viewModel.isUseTls()).isFalse();
    }

    @Test
    void userNameDefaultsToClientSetting() {
      ClientSetting.emailUsername.setValueAndFlush("user-name".toCharArray());
      final var viewModel = new EmailSenderEditorViewModel(view);

      assertThat(viewModel.getEmailUsername()).isEqualTo("user-name");
    }

    @Test
    void noClientSettingsPasswordWillSetBlankPassword() {
      ClientSetting.emailPassword.resetValue();
      final var viewModel = new EmailSenderEditorViewModel(view);

      assertThat(String.valueOf(viewModel.getEmailPassword())).isEmpty();
    }

    @Test
    void rememberPasswordWillDefaultToClientSetting() {
      ClientSetting.rememberEmailPassword.setValueAndFlush(true);
      final var viewModel = new EmailSenderEditorViewModel(view);

      assertThat(viewModel.isForgetPasswordOnShutdown()).isFalse();
    }
  }

  @Nested
  class VerifyValuesPersistedToClientSettings {

    @Test
    void smtpServer() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setSmtpServer("server value");
      assertThat(ClientSetting.emailServerHost.getValueOrThrow()).isEqualTo("server value");
    }

    @Test
    void useTls() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setUseTls(false);
      assertThat(ClientSetting.emailServerSecurity.getValueOrThrow()).isFalse();
    }

    @Test
    void smtpPort() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setSmtpPort("333");
      assertThat(ClientSetting.emailServerPort.getValueOrThrow()).isEqualTo(333);
    }

    @Test
    void invalidSmtpPortValueIsNotPersisted() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setSmtpPort("-1");
      assertThat(ClientSetting.emailServerPort.getValue()).isEmpty();
    }

    @Test
    @DisplayName("Verify generic provider sets email provider and clears host and port")
    void genericProviderClientSettings() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setSelectedProvider(GENERIC_SMTP);
      assertGenericSmtpProviderSettings();
    }

    private void assertGenericSmtpProviderSettings() {
      assertThat(ClientSetting.emailProvider.getValueOrThrow()).isEqualTo(GENERIC_SMTP);
      assertThat(ClientSetting.emailServerSecurity.getValueOrThrow()).isTrue();
      assertThat(ClientSetting.emailServerHost.getValue()).isEmpty();
      assertThat(ClientSetting.emailServerPort.getValue()).isEmpty();
    }

    @Test
    @DisplayName("Verify email provider preset sets email provider, host and port")
    void emailProviderPreset() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setSelectedProvider(GMAIL.getName());
      assertThat(ClientSetting.emailProvider.getValueOrThrow()).isEqualTo(GMAIL.getName());
      assertThat(ClientSetting.emailServerSecurity.getValueOrThrow()).isTrue();
      assertThat(ClientSetting.emailServerHost.getValue()).contains(GMAIL.getServer());
      assertThat(ClientSetting.emailServerPort.getValue()).contains(GMAIL.getPort());
    }

    @Test
    @DisplayName(
        "Verify that settings are updated even if we just selected a preset email provider")
    void changingToGenericSmtpSettingResetsClientSettings() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setSelectedProvider(GMAIL.getName());
      viewModel.setSelectedProvider(GENERIC_SMTP);
      assertGenericSmtpProviderSettings();
    }

    @Test
    void username() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setEmailUsername("user");
      assertThat(ClientSetting.emailUsername.getValueOrThrow()).isEqualTo("user".toCharArray());
    }

    @Test
    void password() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setEmailPassword("password123".toCharArray());
      assertThat(ClientSetting.emailPassword.getValueOrThrow())
          .isEqualTo("password123".toCharArray());
    }

    @Test
    void rememberPassword() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setRememberPassword(true);
      assertThat(ClientSetting.rememberEmailPassword.getValueOrThrow()).isTrue();
    }
  }

  @Nested
  class VisibilityControls {

    @DisplayName("Verify show server options is false when email provider is not generic smtp")
    @ParameterizedTest
    @ValueSource(strings = {"unknown", "Gmail", EmailSenderEditorViewModel.PROVIDER_DISABLED})
    void showServerOptionsNegativeCases(final String provider) {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setSelectedProvider(provider);

      assertThat(viewModel.showServerOptions()).isFalse();
    }

    @DisplayName("Verify show server options is true when email provider is generic smtp")
    @Test
    void showServerOptionsPositive() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setSelectedProvider(GENERIC_SMTP);

      assertThat(viewModel.showServerOptions()).isTrue();
    }

    @DisplayName("Verify show email options is false when email provider is disabled")
    @Test
    void showEmailOptionsNegativeCases() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setSelectedProvider(EmailSenderEditorViewModel.PROVIDER_DISABLED);
      assertThat(viewModel.showEmailOptions()).isFalse();
    }

    @DisplayName("Verify show email options is true when email provider is not disabled")
    @ParameterizedTest
    @ValueSource(strings = {"unknown", "Gmail", GENERIC_SMTP})
    void showEmailOptionsPositiveCases(final String providers) {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setSelectedProvider(providers);
      assertThat(viewModel.showEmailOptions()).isTrue();
    }
  }

  @Nested
  class PopulateFromGameProperties {
    @Mock private GameProperties gameProperties;

    @Test
    void toField() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      when(gameProperties.get(eq(IEmailSender.RECIPIENTS), any(String.class))).thenReturn("to");
      when(gameProperties.get(eq(IEmailSender.SUBJECT), any(String.class))).thenReturn("subject");
      when(gameProperties.get(IEmailSender.POST_AFTER_COMBAT, false)).thenReturn(true);

      viewModel.populateFromGameProperties(gameProperties);

      assertThat(viewModel.getToAddress()).isEqualTo("to");
      assertThat(viewModel.getSubject()).isEqualTo("subject");
      assertThat(viewModel.isSendEmailAfterCombatMove()).isTrue();
    }

    @Test
    void populatingFromGamePropertiesInvokesViewCallback() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.populateFromGameProperties(gameProperties);
      verify(view).viewModelChanged(viewModel);
    }
  }

  @Nested
  class FieldValidation {

    @Test
    void allFieldsValidWhenProviderIsDisabled() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setSelectedProvider(EmailSenderEditorViewModel.PROVIDER_DISABLED);

      assertThat(viewModel.isSmtpServerValid()).isTrue();
      assertThat(viewModel.isSmtpPortValid()).isTrue();
      assertThat(viewModel.isToAddressValid()).isTrue();
      assertThat(viewModel.isSubjectValid()).isTrue();
      assertThat(viewModel.isUsernameValid()).isTrue();
      assertThat(viewModel.isPasswordValid()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void smtpServerInvalidWhenNotSet(final String blankValue) {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setSelectedProvider(GENERIC_SMTP);
      viewModel.setSmtpServer(blankValue);

      assertThat(viewModel.isSmtpServerValid()).isFalse();
    }

    @Test
    void smtpServerValidWhenSet() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setSelectedProvider(GENERIC_SMTP);
      viewModel.setSmtpServer("-");

      assertThat(viewModel.isSmtpServerValid()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "-", "-1", "0", "111111111111333", "value", "one"})
    void smtpPortInvalidWhenNotPositiveInteger(final String notValid) {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setSelectedProvider(GENERIC_SMTP);
      viewModel.setSmtpPort(notValid);

      assertThat(viewModel.isSmtpPortValid()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "100", "3330"})
    void smtpPortValidWhenIsSetToPositiveInteger(final String valid) {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setSelectedProvider(GENERIC_SMTP);
      viewModel.setSmtpPort(valid);

      assertThat(viewModel.isSmtpPortValid()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "-", "not-an-email", "@", "a@", "@a"})
    void toAddressMustBeValidEmailSet(final String invalidEmail) {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setSelectedProvider(GENERIC_SMTP);
      viewModel.setToAddress(invalidEmail);

      assertThat(viewModel.isToAddressValid()).isFalse();
    }

    @Test
    void toAddressValidWhenSetToValidEmail() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setSelectedProvider(GENERIC_SMTP);
      viewModel.setToAddress("valid@email.com");

      assertThat(viewModel.isToAddressValid()).isTrue();
    }

    @Test
    void subjectNotValidWhenNotSet() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setSelectedProvider(GENERIC_SMTP);
      viewModel.setSubject("");

      assertThat(viewModel.isSubjectValid()).isFalse();
    }

    @Test
    void subjectValidWhenSet() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setSelectedProvider(GENERIC_SMTP);
      viewModel.setSubject("-");

      assertThat(viewModel.isSubjectValid()).isTrue();
    }

    @Test
    void usernameValidWhenSet() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setEmailUsername("name");

      assertThat(viewModel.isUsernameValid()).isTrue();
    }

    @Test
    void passwordValidWhenSet() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setEmailPassword("password".toCharArray());

      assertThat(viewModel.isPasswordValid()).isTrue();
    }
  }

  @Nested
  class AllFieldsValid {

    @Test
    void allFieldsAreValidIfProviderIsDisabled() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setSelectedProvider(EmailSenderEditorViewModel.PROVIDER_DISABLED);
      assertThat(viewModel.areFieldsValid()).isTrue();
    }

    @Test
    @DisplayName(
        "All fields valid when we have a SMTP server, credentials, a toAddress and subject line")
    void fieldsValidWhenSmtpSettingsAreSetAndSubjectAndToFieldsAreSet() {
      final var viewModel = givenGenericProviderWithValidSettings();
      ClientSetting.flush();

      assertThat(viewModel.areFieldsValid()).isTrue();
    }

    private EmailSenderEditorViewModel givenGenericProviderWithValidSettings() {
      final var viewModel = new EmailSenderEditorViewModel(view);
      viewModel.setSelectedProvider(GENERIC_SMTP);

      viewModel.setSmtpServer("server");
      viewModel.setSmtpPort("200");
      viewModel.setEmailUsername("username");
      viewModel.setEmailPassword("password".toCharArray());

      viewModel.setToAddress("to@to");
      viewModel.setSubject("subject");
      return viewModel;
    }

    @Test
    void allFieldsNotValidWhenMissingHost() {
      final var viewModel = givenGenericProviderWithValidSettings();
      ClientSetting.emailServerHost.resetValue();
      ClientSetting.flush();

      assertThat(viewModel.areFieldsValid()).isFalse();
    }

    @Test
    void allFieldsNotValidWhenMissingPort() {
      final var viewModel = givenGenericProviderWithValidSettings();
      ClientSetting.emailServerPort.resetValue();
      ClientSetting.flush();

      assertThat(viewModel.areFieldsValid()).isFalse();
    }

    @Test
    void allFieldsNotValidWhenMissingUsername() {
      final var viewModel = givenGenericProviderWithValidSettings();
      viewModel.setEmailUsername("");

      assertThat(viewModel.areFieldsValid()).isFalse();
    }

    @Test
    void allFieldsNotValidWhenMissingPassword() {
      final var viewModel = givenGenericProviderWithValidSettings();
      viewModel.setEmailPassword(new char[0]);

      assertThat(viewModel.areFieldsValid()).isFalse();
    }

    @Test
    void allFieldsNotValidWhenToAddressIsNotValidEmail() {
      final var viewModel = givenGenericProviderWithValidSettings();
      ClientSetting.flush();
      viewModel.setToAddress("to");

      assertThat(viewModel.areFieldsValid()).isFalse();
    }

    @Test
    void allFieldsNotValidWhenSubjectIsMissing() {
      final var viewModel = givenGenericProviderWithValidSettings();
      ClientSetting.flush();
      viewModel.setSubject(" ");

      assertThat(viewModel.areFieldsValid()).isFalse();
    }
  }
}
