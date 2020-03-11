package games.strategy.engine.framework.startup.ui.posted.game.pbem;

import games.strategy.engine.data.properties.GameProperties;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import org.triplea.java.ViewModelListener;
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
          .textListener(
              toAddress -> {
                if (syncToModel) {
                  viewModel.setToAddress(toAddress);
                }
              })
          .columns(FIELD_LENGTH)
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
    syncToModel = true;
  }

  JPanel build() {
    toggleFieldVisibility();

    final JPanel contents =
        new JPanelBuilder()
            .border(new TitledBorder("Automatically Send Emails"))
            .gridBagLayout()
            .build();
    int row = 0;

    row++;
    contents.add(new JLabel("Email Provider"), new GridBagConstraintsBuilder(0, row).build());
    contents.add(emailProviderSelectionBox, new GridBagConstraintsBuilder(1, row).build());

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
    contents.add(
        sendEmailAfterCombatMoveCheckBox,
        new GridBagConstraintsBuilder(0, row).gridWidth(2).build());

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

          sendEmailAfterCombatMoveCheckBox.setSelected(viewModel.isSendEmailAfterCombatMove());
          syncToModel = true;
        });
  }

  private static void updateTextFieldIfNeeded(
      final JTextField textField, final String incomingValue) {
    // avoid updating fields to the same value; setting field text
    // resets the carat position to the end, very annoying if a user is
    // editing text.
    if (!textField.getText().equals(incomingValue)) {
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
}
