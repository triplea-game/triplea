package games.strategy.engine.framework.startup.ui.posted.game.pbem;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.startup.ui.posted.game.HelpTexts;
import games.strategy.engine.posted.game.pbem.IEmailSender;
import games.strategy.triplea.settings.ClientSetting;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import org.triplea.domain.data.PlayerEmailValidation;
import org.triplea.java.StringUtils;
import org.triplea.java.ViewModelListener;

class EmailSenderEditorViewModel {
  static final String PROVIDER_DISABLED = "Disabled";
  static final String GENERIC_SMTP = "Generic SMTP";

  private final ViewModelListener<EmailSenderEditorViewModel> view;

  @Setter(onMethod_ = @VisibleForTesting)
  private SendTestEmailAction sendTestEmailAction = new SendTestEmailAction();

  @Getter private String selectedProvider;
  @Getter private boolean useTls;
  @Getter private String smtpServer;
  @Getter private String smtpPort;
  @Getter private String subject = "";
  @Getter private String toAddress = "";
  @Getter @Setter private boolean sendEmailAfterCombatMove;
  @Getter private String emailUsername;
  private boolean emailPasswordIsSet;
  private boolean rememberPassword;
  @Setter private Runnable validatedFieldsChangedListener;

  EmailSenderEditorViewModel(final ViewModelListener<EmailSenderEditorViewModel> view) {
    this.view = view;
    selectedProvider = ClientSetting.emailProvider.getValue().orElse(PROVIDER_DISABLED);
    smtpServer = ClientSetting.emailServerHost.getValue().orElse("");
    smtpPort = ClientSetting.emailServerPort.getValue().map(String::valueOf).orElse("");
    useTls = ClientSetting.emailServerSecurity.getValue().orElse(true);
    emailUsername = ClientSetting.emailUsername.getValue().map(String::valueOf).orElse("");
    emailPasswordIsSet = ClientSetting.emailPassword.isSet();
    rememberPassword = ClientSetting.rememberEmailPassword.getValue().orElse(false);
  }

  String getEmailPassword() {
    return emailPasswordIsSet ? "********" : "";
  }

  static Collection<String> getProviderOptions() {
    return List.of(
        PROVIDER_DISABLED,
        EmailProviderPreset.GMAIL.getName(),
        EmailProviderPreset.HOTMAIL.getName(),
        GENERIC_SMTP);
  }

  boolean isTestEmailButtonEnabled() {
    return smtpServer != null
        && !smtpServer.isBlank()
        && !toAddress.isBlank()
        && PlayerEmailValidation.areValid(toAddress)
        && StringUtils.isPositiveInt(smtpPort)
        && ClientSetting.emailUsername.isSet()
        && ClientSetting.emailUsername.getValueOrThrow().length > 0
        && ClientSetting.emailPassword.isSet()
        && ClientSetting.emailPassword.getValueOrThrow().length > 0;
  }

  void setSelectedProvider(final String provider) {
    Preconditions.checkNotNull(provider);
    if (!selectedProvider.equals(provider)) {
      selectedProvider = provider;
      ClientSetting.emailProvider.setValueAndFlush(selectedProvider);

      EmailProviderPreset.lookupByName(provider)
          .ifPresentOrElse(
              preset -> {
                smtpServer = preset.getServer();
                smtpPort = String.valueOf(preset.getPort());
                useTls = preset.isUseTlsByDefault();

                ClientSetting.emailServerHost.setValue(smtpServer);
                ClientSetting.emailServerPort.setValue(preset.getPort());
                ClientSetting.emailServerSecurity.setValueAndFlush(useTls);
              },
              () -> {
                smtpServer = "";
                smtpPort = "";
                useTls = true;

                ClientSetting.emailServerHost.resetValue();
                ClientSetting.emailServerPort.resetValue();
                ClientSetting.emailServerSecurity.setValueAndFlush(useTls);
              });

      invokeCallbacks();
    }
  }

  private void invokeCallbacks() {
    view.viewModelChanged(this);
    Optional.ofNullable(validatedFieldsChangedListener).ifPresent(Runnable::run);
  }

  boolean showServerOptions() {
    return selectedProvider.equals(GENERIC_SMTP);
  }

  boolean showEmailOptions() {
    return !isEmailProviderDisabled();
  }

  private boolean isEmailProviderDisabled() {
    return selectedProvider.equals(PROVIDER_DISABLED);
  }

  String getEmailHelpText() {
    if (selectedProvider.equals(EmailSenderEditorViewModel.PROVIDER_DISABLED)) {
      return HelpTexts.SMTP_DISABLED;
    } else if (selectedProvider.equals(EmailProviderPreset.GMAIL.getName())) {
      return HelpTexts.gmailHelpText();
    } else if (selectedProvider.equals(EmailProviderPreset.HOTMAIL.getName())) {
      return HelpTexts.hotmailHelpText();
    } else {
      return HelpTexts.GENERIC_SMTP_SERVER;
    }
  }

  void setSmtpServer(final String smtpServer) {
    Preconditions.checkNotNull(smtpServer);
    if (!this.smtpServer.equals(smtpServer)) {
      this.smtpServer = smtpServer.trim();
      ClientSetting.emailServerHost.setValueAndFlush(smtpServer);
      invokeCallbacks();
    }
  }

  boolean isSmtpServerValid() {
    return isEmailProviderDisabled() || !smtpServer.isBlank();
  }

  void setSmtpPort(final String smtpPort) {
    Preconditions.checkNotNull(smtpPort);
    if (!this.smtpPort.equals(smtpPort)) {
      this.smtpPort = smtpPort;
      if (StringUtils.isPositiveInt(smtpPort)) {
        ClientSetting.emailServerPort.setValueAndFlush(Integer.valueOf(smtpPort.trim()));
      } else {
        ClientSetting.emailServerPort.resetValue();
        ClientSetting.flush();
      }
      invokeCallbacks();
    }
  }

  boolean isSmtpPortValid() {
    return isEmailProviderDisabled() || StringUtils.isPositiveInt(smtpPort);
  }

  void setSubject(final String subject) {
    Preconditions.checkNotNull(subject);
    if (!this.subject.equals(subject)) {
      this.subject = subject.trim();
      invokeCallbacks();
    }
  }

  boolean isSubjectValid() {
    return isEmailProviderDisabled() || !subject.isBlank();
  }

  void setToAddress(final String toAddress) {
    Preconditions.checkNotNull(toAddress);
    if (!this.toAddress.equals(toAddress)) {
      this.toAddress = toAddress.trim();
      invokeCallbacks();
    }
  }

  boolean isToAddressValid() {
    return isEmailProviderDisabled()
        || (!toAddress.isBlank() && PlayerEmailValidation.areValid(toAddress));
  }

  void setUseTls(final boolean useTls) {
    this.useTls = useTls;
    ClientSetting.emailServerSecurity.setValueAndFlush(useTls);
  }

  void setEmailUsername(final String value) {
    Preconditions.checkNotNull(value);
    if (!this.emailUsername.equals(value)) {
      this.emailUsername = value;
      ClientSetting.emailUsername.setValueAndFlush(value.toCharArray());
      invokeCallbacks();
    }
  }

  boolean isUsernameValid() {
    return isEmailProviderDisabled() || !emailUsername.isBlank();
  }

  void setEmailPassword(final char[] emailPassword) {
    // note, we do not store the email password locally so that it can be in one few places
    // on the system.
    Preconditions.checkNotNull(emailPassword);
    if (emailPassword.length == 0) {
      emailPasswordIsSet = false;
      ClientSetting.emailPassword.resetValue();
      ClientSetting.flush();
    } else {
      emailPasswordIsSet = true;
      ClientSetting.emailPassword.setValueAndFlush(emailPassword);
    }
    invokeCallbacks();
  }

  boolean isPasswordValid() {
    return isEmailProviderDisabled() || emailPasswordIsSet;
  }

  boolean isForgetPasswordOnShutdown() {
    return !rememberPassword;
  }

  void sendTestEmail() {
    sendTestEmailAction.send(toAddress.trim());
  }

  void setRememberPassword(final boolean rememberPassword) {
    if (this.rememberPassword != rememberPassword) {
      this.rememberPassword = rememberPassword;
      ClientSetting.rememberEmailPassword.setValueAndFlush(rememberPassword);
    }
  }

  void populateFromGameProperties(final GameProperties properties) {
    subject = properties.get(IEmailSender.SUBJECT, "");
    toAddress = properties.get(IEmailSender.RECIPIENTS, "");
    sendEmailAfterCombatMove = properties.get(IEmailSender.POST_AFTER_COMBAT, false);
    invokeCallbacks();
  }

  void applyToGameProperties(final GameProperties properties) {
    properties.set(IEmailSender.SUBJECT, subject);
    properties.set(IEmailSender.RECIPIENTS, toAddress);
    properties.set(IEmailSender.POST_AFTER_COMBAT, sendEmailAfterCombatMove);
  }

  boolean areFieldsValid() {
    return selectedProvider.equals(PROVIDER_DISABLED)
        || (isToAddressValid()
            && isSubjectValid()
            && ClientSetting.emailServerHost.getValue().map(value -> !value.isBlank()).orElse(false)
            && ClientSetting.emailServerPort.getValue().map(value -> value > 0).orElse(false)
            && ClientSetting.emailUsername.getValue().map(value -> value.length > 0).orElse(false)
            && ClientSetting.emailPassword.getValue().map(value -> value.length > 0).orElse(false));
  }
}
