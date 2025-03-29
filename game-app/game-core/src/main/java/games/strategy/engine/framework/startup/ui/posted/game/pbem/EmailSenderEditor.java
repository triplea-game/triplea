package games.strategy.engine.framework.startup.ui.posted.game.pbem;

import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.startup.ui.posted.game.HelpTexts;
import games.strategy.triplea.settings.ClientSetting;
import java.awt.*;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import org.triplea.domain.data.LobbyConstants;
import org.triplea.java.ViewModelListener;
import org.triplea.swing.DocumentListenerBuilder;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JCheckBoxBuilder;
import org.triplea.swing.JComboBoxBuilder;
import org.triplea.swing.JTextFieldBuilder;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.GridBagConstraintsBuilder;
import org.triplea.swing.jpanel.JPanelBuilder;

/** An editor for modifying email senders. */
public class EmailSenderEditor implements ViewModelListener<EmailSenderEditorViewModel> {

  private static final int FIELD_LENGTH = 20;
  // 254 * 4 + 3 = 1019 Three max length emails with 3 spaces to separate.
  public static final int TO_ADDRESS_MAX_LENGTH = 1019;
  private final EmailSenderEditorViewModel viewModel = new EmailSenderEditorViewModel(this);

  private boolean syncToModel;

  private final JComboBox<String> emailProviderSelectionBox =
      JComboBoxBuilder.builder()
          .items(EmailSenderEditorViewModel.getProviderOptions())
          .selectedItem(viewModel.getSelectedProvider())
          .itemSelectedAction(
              provider -> {
                if (syncToModel) {
                  viewModel.setSelectedProvider(provider);
                }
              })
          .build();

  private final JPanel contents =
      new JPanelBuilder()
          .border(new TitledBorder("Automatically Send Emails"))
          .gridBagLayout()
          .build();

  private final JButton helpButton =
      new JButtonBuilder("Help")
          .actionListener(
              () ->
                  JOptionPane.showMessageDialog(
                      contents,
                      new JLabel(viewModel.getEmailHelpText()),
                      "Play By Email Help",
                      JOptionPane.INFORMATION_MESSAGE))
          .toolTip("Click this button to show help text")
          .build();

  private final JLabel smtpServerLabel = new JLabel("SMTP Server");
  private final JTextField smtpServerField =
      JTextFieldBuilder.builder()
          .text(viewModel.getSmtpServer())
          .textListener(
              server -> {
                if (syncToModel) {
                  viewModel.setSmtpServer(server);
                }
              })
          .columns(FIELD_LENGTH)
          .build();

  private final JLabel smtpPortLabel = new JLabel("Port");
  private final JTextField smtpPortField =
      JTextFieldBuilder.builder()
          .text(viewModel.getSmtpPort())
          .textListener(
              smtpPort -> {
                if (syncToModel) {
                  viewModel.setSmtpPort(smtpPort);
                }
              })
          .columns(FIELD_LENGTH)
          .build();

  private final JCheckBox useTlsCheckBox =
      new JCheckBoxBuilder("Use TLS encryption")
          .selected(viewModel.isUseTls())
          .actionListener(
              useTls -> {
                if (syncToModel) {
                  viewModel.setUseTls(useTls);
                }
              })
          .build();

  private final JLabel subjectLabel = new JLabel("Subject");
  private final JTextField subjectField =
      JTextFieldBuilder.builder()
          .text(viewModel.getSubject())
          .textListener(
              subject -> {
                if (syncToModel) {
                  viewModel.setSubject(subject);
                }
              })
          .columns(FIELD_LENGTH)
          .build();

  private final JLabel toAddressLabel = new JLabel("To:");
  private final JTextField toAddressField =
      JTextFieldBuilder.builder()
          .text(viewModel.getToAddress())
          .maxLength(TO_ADDRESS_MAX_LENGTH)
          .textListener(
              toAddress -> {
                if (syncToModel) {
                  viewModel.setToAddress(toAddress);
                }
              })
          .columns(FIELD_LENGTH)
          .build();

  private final JLabel userNameLabel = new JLabel("Email Username:");
  private final JTextField userNameField =
      JTextFieldBuilder.builder()
          .maxLength(LobbyConstants.USERNAME_MAX_LENGTH)
          .text(viewModel.getEmailUsername())
          .textListener(
              fieldValue -> {
                if (syncToModel) {
                  viewModel.setEmailUsername(fieldValue);
                }
              })
          .columns(FIELD_LENGTH)
          .build();

  private final JLabel passwordLabel = new JLabel("Email Password:");
  private final JPasswordField passwordField =
      new JPasswordField(viewModel.getEmailPassword(), FIELD_LENGTH);

  private final JCheckBox rememberPassword =
      new JCheckBoxBuilder("Remember Password")
          .actionListener(viewModel::setRememberPassword)
          .bind(ClientSetting.rememberEmailPassword)
          .build();
  private final JButton rememberPasswordHelpButton =
      new JButtonBuilder("Help")
          .actionListener(
              button ->
                  JOptionPane.showMessageDialog(
                      button,
                      HelpTexts.rememberPlayByEmailPassword(),
                      "Remember Password",
                      JOptionPane.INFORMATION_MESSAGE))
          .build();

  private final JCheckBox sendEmailAfterCombatMoveCheckBox =
      new JCheckBoxBuilder("Also Send Email After Combat Move")
          .selected(viewModel.isSendEmailAfterCombatMove())
          .actionListener(
              sendEmailAfterCombatMove -> {
                if (syncToModel) {
                  viewModel.setSendEmailAfterCombatMove(sendEmailAfterCombatMove);
                }
              })
          .build();

  private final JButton testEmailButton =
      new JButtonBuilder("Send Test Email")
          .enabled(viewModel.isTestEmailButtonEnabled())
          .actionListener(viewModel::sendTestEmail)
          .build();

  public EmailSenderEditor(final Runnable readyCallback) {
    viewModel.setValidatedFieldsChangedListener(readyCallback);
    this.viewModelChanged(viewModel);

    new DocumentListenerBuilder(() -> viewModel.setEmailPassword(passwordField.getPassword()))
        .attachTo(passwordField);
    syncToModel = true;
  }

  JPanel build() {
    toggleFieldVisibility();

    int row = 0;

    row++;
    contents.add(new JLabel("Email Provider"), new GridBagConstraintsBuilder(0, row).build());
    contents.add(emailProviderSelectionBox, new GridBagConstraintsBuilder(1, row).build());
    contents.add(helpButton, new GridBagConstraintsBuilder(2, row).build());

    row++;
    contents.add(smtpServerLabel, new GridBagConstraintsBuilder(0, row).build());
    contents.add(smtpServerField, new GridBagConstraintsBuilder(1, row).build());

    row++;
    contents.add(smtpPortLabel, new GridBagConstraintsBuilder(0, row).build());
    contents.add(smtpPortField, new GridBagConstraintsBuilder(1, row).build());

    row++;
    contents.add(useTlsCheckBox, new GridBagConstraintsBuilder(0, row).build());

    row++;
    contents.add(toAddressLabel, new GridBagConstraintsBuilder(0, row).build());
    contents.add(toAddressField, new GridBagConstraintsBuilder(1, row).build());
    contents.add(testEmailButton, new GridBagConstraintsBuilder(2, row).build());

    row++;
    contents.add(new JPanel(), new GridBagConstraintsBuilder(0, row).gridWidth(2).build());

    row++;
    contents.add(subjectLabel, new GridBagConstraintsBuilder(0, row).build());
    contents.add(subjectField, new GridBagConstraintsBuilder(1, row).build());

    row++;
    contents.add(userNameLabel, new GridBagConstraintsBuilder(0, row).build());
    contents.add(userNameField, new GridBagConstraintsBuilder(1, row).build());

    row++;
    contents.add(passwordLabel, new GridBagConstraintsBuilder(0, row).build());
    contents.add(passwordField, new GridBagConstraintsBuilder(1, row).build());

    row++;
    contents.add(
        new JPanelBuilder()
            .boxLayoutHorizontal()
            .add(rememberPassword)
            .add(rememberPasswordHelpButton)
            .build(),
        new GridBagConstraintsBuilder(1, row).build());

    row++;
    contents.add(
        sendEmailAfterCombatMoveCheckBox,
        new GridBagConstraintsBuilder(1, row).gridWidth(2).build());

    return contents;
  }

  @Override
  public void viewModelChanged(final EmailSenderEditorViewModel viewModel) {
    syncToModel = false;
    SwingUtilities.invokeLater(
        () -> {
          toggleFieldVisibility();
          emailProviderSelectionBox.setSelectedItem(viewModel.getSelectedProvider());
          SwingComponents.highlightLabelIfNotValid(viewModel.isSmtpServerValid(), smtpServerLabel);
          updateTextFieldIfNeeded(smtpServerField, viewModel.getSmtpServer());

          SwingComponents.highlightLabelIfNotValid(viewModel.isSmtpPortValid(), smtpPortLabel);
          updateTextFieldIfNeeded(smtpPortField, viewModel.getSmtpPort());
          useTlsCheckBox.setSelected(viewModel.isUseTls());

          SwingComponents.highlightLabelIfNotValid(viewModel.isToAddressValid(), toAddressLabel);
          updateTextFieldIfNeeded(toAddressField, viewModel.getToAddress());

          testEmailButton.setEnabled(viewModel.isTestEmailButtonEnabled());

          SwingComponents.highlightLabelIfNotValid(viewModel.isSubjectValid(), subjectLabel);
          updateTextFieldIfNeeded(subjectField, viewModel.getSubject());

          SwingComponents.highlightLabelIfNotValid(viewModel.isUsernameValid(), userNameLabel);
          updateTextFieldIfNeeded(userNameField, viewModel.getEmailUsername());

          SwingComponents.highlightLabelIfNotValid(viewModel.isPasswordValid(), passwordLabel);

          sendEmailAfterCombatMoveCheckBox.setSelected(viewModel.isSendEmailAfterCombatMove());
          syncToModel = true;
        });
  }

  private static void updateTextFieldIfNeeded(
      final JTextField textField, final String incomingValue) {
    // avoid updating fields to the same value; setting field text
    // resets the carat position to the end, very annoying if a user is
    // editing text. Trim the text before comparing as the model will
    // trim email addresses internally, which breaks typing multiple
    // space-separated emails in the To field.
    if (!textField.getText().trim().equals(incomingValue)) {
      textField.setText(incomingValue);
    }
  }

  private void toggleFieldVisibility() {
    smtpServerLabel.setVisible(viewModel.showServerOptions());
    smtpServerField.setVisible(viewModel.showServerOptions());
    smtpPortLabel.setVisible(viewModel.showServerOptions());
    smtpPortField.setVisible(viewModel.showServerOptions());
    useTlsCheckBox.setVisible(viewModel.showServerOptions());
    testEmailButton.setVisible(viewModel.showEmailOptions());
    subjectField.setVisible(viewModel.showEmailOptions());
    subjectLabel.setVisible(viewModel.showEmailOptions());
    toAddressLabel.setVisible(viewModel.showEmailOptions());
    toAddressField.setVisible(viewModel.showEmailOptions());
    userNameLabel.setVisible(viewModel.showEmailOptions());
    userNameField.setVisible(viewModel.showEmailOptions());
    passwordLabel.setVisible(viewModel.showEmailOptions());
    passwordField.setVisible(viewModel.showEmailOptions());
    rememberPassword.setVisible(viewModel.showEmailOptions());
    rememberPasswordHelpButton.setVisible(viewModel.showEmailOptions());
    sendEmailAfterCombatMoveCheckBox.setVisible(viewModel.showEmailOptions());
  }

  void populateFromGameProperties(final GameProperties properties) {
    viewModel.populateFromGameProperties(properties);
  }

  void applyToGameProperties(final GameProperties properties) {
    viewModel.applyToGameProperties(properties);
  }

  boolean areFieldsValid() {
    return viewModel.areFieldsValid();
  }

  boolean isForgetPasswordOnShutdown() {
    return viewModel.isForgetPasswordOnShutdown();
  }
}
