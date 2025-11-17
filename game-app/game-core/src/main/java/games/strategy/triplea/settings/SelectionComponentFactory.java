package games.strategy.triplea.settings;

import static org.triplea.util.Arrays.withSensitiveArray;
import static org.triplea.util.Arrays.withSensitiveArrayAndReturn;

import com.google.common.base.Strings;
import games.strategy.engine.framework.startup.ui.posted.game.DiceServerEditor;
import games.strategy.engine.framework.startup.ui.posted.game.pbem.EmailProviderPreset;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.engine.posted.game.pbf.NodeBbTokenGenerator;
import games.strategy.engine.posted.game.pbf.NodeBbTokenGenerator.TokenInfo;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JComboBoxBuilder;
import org.triplea.swing.JTextFieldBuilder;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.JPanelBuilder;

/**
 * Logic for building UI components that "bind" to ClientSettings. For example, if we have a setting
 * that needs a number, we could create an integer text field with this class. This class takes care
 * of the UI code to ensure we render the proper swing component with validation.
 */
final class SelectionComponentFactory {
  private SelectionComponentFactory() {}

  static SelectionComponent<JComponent> proxySettings(
      final ClientSetting<HttpProxy.ProxyChoice> proxyChoiceClientSetting,
      final ClientSetting<String> proxyHostClientSetting,
      final ClientSetting<Integer> proxyPortClientSetting) {
    return new SelectionComponent<>() {
      final HttpProxy.ProxyChoice proxyChoice = proxyChoiceClientSetting.getValueOrThrow();
      final JRadioButton noneButton =
          new JRadioButton("None", proxyChoice == HttpProxy.ProxyChoice.NONE);
      final JRadioButton systemButton =
          new JRadioButton(
              "Use System Settings", proxyChoice == HttpProxy.ProxyChoice.USE_SYSTEM_SETTINGS);
      final JRadioButton userButton =
          new JRadioButton(
              "Use These Settings:", proxyChoice == HttpProxy.ProxyChoice.USE_USER_PREFERENCES);
      final JTextField hostText = new JTextField(proxyHostClientSetting.getValue().orElse(""), 20);
      final JTextField portText =
          new JTextField(proxyPortClientSetting.getValue().map(Object::toString).orElse(""), 6);
      final JPanel radioPanel =
          new JPanelBuilder()
              .boxLayoutVertical()
              .addLeftJustified(noneButton)
              .addLeftJustified(systemButton)
              .addLeftJustified(userButton)
              .addLeftJustified(
                  new JPanelBuilder()
                      .boxLayoutHorizontal()
                      .addHorizontalStrut(getRadioButtonLabelHorizontalOffset())
                      .add(
                          new JPanelBuilder()
                              .boxLayoutHorizontal()
                              .addLeftJustified(new JLabel("Proxy Host:"))
                              .addLeftJustified(hostText)
                              .build())
                      .build())
              .addLeftJustified(
                  new JPanelBuilder()
                      .boxLayoutHorizontal()
                      .addHorizontalStrut(getRadioButtonLabelHorizontalOffset())
                      .add(
                          new JPanelBuilder()
                              .boxLayoutHorizontal()
                              .addLeftJustified(new JLabel("Proxy Port:"))
                              .addLeftJustified(portText)
                              .build())
                      .build())
              .build();
      final ActionListener enableUserSettings =
          e -> {
            if (userButton.isSelected()) {
              hostText.setEnabled(true);
              portText.setEnabled(true);
            } else {
              hostText.setEnabled(false);
              hostText.setText("");
              portText.setEnabled(false);
              portText.setText("");
            }
          };

      @Override
      public JComponent getUiComponent() {
        SwingComponents.assignToButtonGroup(noneButton, systemButton, userButton);
        enableUserSettings.actionPerformed(null);
        userButton.addActionListener(enableUserSettings);
        noneButton.addActionListener(enableUserSettings);
        systemButton.addActionListener(enableUserSettings);

        return radioPanel;
      }

      @Override
      public void save(final SaveContext context) {
        if (noneButton.isSelected()) {
          context.setValue(proxyChoiceClientSetting, HttpProxy.ProxyChoice.NONE);
          context.setValue(proxyHostClientSetting, null);
          context.setValue(proxyPortClientSetting, null);
        } else if (systemButton.isSelected()) {
          context.setValue(proxyChoiceClientSetting, HttpProxy.ProxyChoice.USE_SYSTEM_SETTINGS);
          context.setValue(proxyHostClientSetting, null);
          context.setValue(proxyPortClientSetting, null);
          HttpProxy.updateSystemProxy();
        } else {
          final String encodedHost = hostText.getText().trim();
          final Optional<String> optionalHost = parseHost(encodedHost);
          if (optionalHost.isEmpty()) {
            context.reportError(
                proxyHostClientSetting,
                "must be a network name or an IP address",
                Strings.emptyToNull(encodedHost));
          }

          final String encodedPort = portText.getText().trim();
          final Optional<Integer> optionalPort = parsePort(encodedPort);
          if (optionalPort.isEmpty()) {
            context.reportError(
                proxyPortClientSetting,
                "must be a positive integer, usually 4 to 5 digits",
                Strings.emptyToNull(encodedPort));
          }

          if (optionalHost.isPresent() && optionalPort.isPresent()) {
            context.setValue(proxyChoiceClientSetting, HttpProxy.ProxyChoice.USE_USER_PREFERENCES);
            context.setValue(proxyHostClientSetting, optionalHost.get());
            context.setValue(proxyPortClientSetting, optionalPort.get());
          }
        }
      }

      private Optional<String> parseHost(final String encodedHost) {
        return !encodedHost.isEmpty() ? Optional.of(encodedHost) : Optional.empty();
      }

      private Optional<Integer> parsePort(final String encodedPort) {
        try {
          final int port = Integer.parseInt(encodedPort);
          return (port > 0) ? Optional.of(port) : Optional.empty();
        } catch (final NumberFormatException e) {
          return Optional.empty();
        }
      }

      @Override
      public void resetToDefault() {
        ClientSetting.flush();
        hostText.setText(proxyHostClientSetting.getDefaultValue().orElse(""));
        portText.setText(proxyPortClientSetting.getDefaultValue().map(Object::toString).orElse(""));
        setProxyChoice(
            proxyChoiceClientSetting.getDefaultValue().orElse(HttpProxy.ProxyChoice.NONE));
      }

      private void setProxyChoice(final HttpProxy.ProxyChoice proxyChoice) {
        noneButton.setSelected(proxyChoice == HttpProxy.ProxyChoice.NONE);
        systemButton.setSelected(proxyChoice == HttpProxy.ProxyChoice.USE_SYSTEM_SETTINGS);
        userButton.setSelected(proxyChoice == HttpProxy.ProxyChoice.USE_USER_PREFERENCES);
        enableUserSettings.actionPerformed(null);
      }

      @Override
      public void reset() {
        ClientSetting.flush();
        hostText.setText(proxyHostClientSetting.getValue().orElse(""));
        portText.setText(proxyPortClientSetting.getValue().map(Object::toString).orElse(""));
        setProxyChoice(proxyChoiceClientSetting.getValueOrThrow());
      }
    };
  }

  private static int getRadioButtonLabelHorizontalOffset() {
    final JRadioButton radioButton = new JRadioButton("\u200B"); // zero-width space
    return radioButton.getPreferredSize().width - radioButton.getInsets().right;
  }

  /** Text field that only accepts numbers between a certain range. */
  static SelectionComponent<JComponent> intValueRange(
      final ClientSetting<Integer> clientSetting, final int lo, final int hi) {
    return intValueRange(clientSetting, lo, hi, false);
  }

  /** Text field that only accepts numbers between a certain range. */
  static SelectionComponent<JComponent> intValueRange(
      final ClientSetting<Integer> clientSetting,
      final int lo,
      final int hi,
      final boolean allowUnset) {
    return new SelectionComponent<>() {
      private final JSpinner component =
          new JSpinner(
              new SpinnerNumberModel(
                  toValidIntValue(clientSetting.getValue()), lo - (allowUnset ? 1 : 0), hi, 1));

      @Override
      public JComponent getUiComponent() {
        return component;
      }

      private int toValidIntValue(final Optional<Integer> value) {
        return value.orElseGet(this::unsetValue);
      }

      private int unsetValue() {
        return lo - 1;
      }

      @Override
      public void save(final SaveContext context) {
        context.setValue(clientSetting, getComponentValue());
      }

      private @Nullable Integer getComponentValue() {
        final int value = (int) component.getValue();
        return (allowUnset && (value == unsetValue())) ? null : value;
      }

      @Override
      public void resetToDefault() {
        component.setValue(toValidIntValue(clientSetting.getDefaultValue()));
      }

      @Override
      public void reset() {
        component.setValue(toValidIntValue(clientSetting.getValue()));
      }
    };
  }

  /** yes/no radio buttons. */
  static SelectionComponent<JComponent> booleanRadioButtons(
      final ClientSetting<Boolean> clientSetting) {
    return new SelectionComponent<>() {
      final boolean initialSelection = clientSetting.getValueOrThrow();
      final JRadioButton yesButton = new JRadioButton("True");
      final JRadioButton noButton = new JRadioButton("False");
      final JPanel buttonPanel =
          new JPanelBuilder().boxLayoutHorizontal().add(yesButton).add(noButton).build();

      @Override
      public JComponent getUiComponent() {
        yesButton.setSelected(initialSelection);
        noButton.setSelected(!initialSelection);
        SwingComponents.assignToButtonGroup(yesButton, noButton);
        return buttonPanel;
      }

      @Override
      public void save(final SaveContext context) {
        context.setValue(clientSetting, yesButton.isSelected());
      }

      @Override
      public void resetToDefault() {
        final boolean value = clientSetting.getDefaultValue().orElse(false);
        yesButton.setSelected(value);
        noButton.setSelected(!value);
      }

      @Override
      public void reset() {
        final boolean value = clientSetting.getValueOrThrow();
        yesButton.setSelected(value);
        noButton.setSelected(!value);
      }
    };
  }

  private static SelectionComponent<JComponent> selectFile(
      final ClientSetting<Path> clientSetting,
      final SwingComponents.FolderSelectionMode folderSelectionMode) {
    return new SelectionComponent<>() {
      final JTextField field =
          new JTextField(SelectionComponentUiUtils.toString(clientSetting.getValue()), 20);
      final JButton button =
          new JButtonBuilder()
              .title("Select")
              .actionListener(
                  buttonSelect ->
                      SwingComponents.showJFileChooser(
                              JOptionPane.getFrameForComponent(buttonSelect), folderSelectionMode)
                          .ifPresent(file -> field.setText(file.toAbsolutePath().toString())))
              .build();

      @Override
      public JComponent getUiComponent() {
        field.setEditable(false);

        return new JPanelBuilder()
            .boxLayoutHorizontal()
            .add(field)
            .addHorizontalStrut(5)
            .add(button)
            .build();
      }

      @Override
      public void save(final SaveContext context) {
        final String value = field.getText();
        context.setValue(clientSetting, value.isEmpty() ? null : Path.of(value));
      }

      @Override
      public void resetToDefault() {
        field.setText(SelectionComponentUiUtils.toString(clientSetting.getDefaultValue()));
      }

      @Override
      public void reset() {
        field.setText(SelectionComponentUiUtils.toString(clientSetting.getValue()));
      }
    };
  }

  /** Folder selection prompt. */
  static SelectionComponent<JComponent> folderPath(final ClientSetting<Path> clientSetting) {
    return selectFile(clientSetting, SwingComponents.FolderSelectionMode.DIRECTORIES);
  }

  static <T, E> SelectionComponent<JComponent> selectionBox(
      final ClientSetting<T> clientSetting,
      final Class<E> comboBoxItemType,
      final Collection<E> comboBoxItems,
      final Function<T, Optional<E>> convertSettingValueToComboBoxItem,
      final Function<E, T> convertComboBoxItemToSettingValue,
      final Function<E, ?> convertComboBoxItemToDisplayValue) {
    return new SelectionComponent<>() {
      private final JComboBox<E> comboBox = newComboBox();

      private JComboBox<E> newComboBox() {
        final JComboBox<E> comboBox = new JComboBox<>();
        comboBoxItems.forEach(comboBox::addItem);
        setComboBoxSelectedItem(comboBox, clientSetting::getValue);
        comboBox.setRenderer(
            new DefaultListCellRenderer() {
              private static final long serialVersionUID = -3094995494539073655L;

              @Override
              public Component getListCellRendererComponent(
                  final JList<?> list,
                  final @Nullable Object value,
                  final int index,
                  final boolean isSelected,
                  final boolean cellHasFocus) {
                return super.getListCellRendererComponent(
                    list,
                    Optional.ofNullable(value)
                        .map(comboBoxItemType::cast)
                        .map(convertComboBoxItemToDisplayValue)
                        .orElse(null),
                    index,
                    isSelected,
                    cellHasFocus);
              }
            });
        return comboBox;
      }

      private void setComboBoxSelectedItem(
          final JComboBox<E> comboBox, final Supplier<Optional<T>> settingValueSupplier) {
        comboBox.setSelectedItem(
            settingValueSupplier.get().flatMap(convertSettingValueToComboBoxItem).orElse(null));
      }

      @Override
      public JComponent getUiComponent() {
        return comboBox;
      }

      @Override
      public void save(final SaveContext context) {
        final @Nullable T value =
            Optional.ofNullable(comboBox.getSelectedItem())
                .map(comboBoxItemType::cast)
                .map(convertComboBoxItemToSettingValue)
                .orElse(null);
        context.setValue(clientSetting, value);
      }

      @Override
      public void resetToDefault() {
        setComboBoxSelectedItem(comboBox, clientSetting::getDefaultValue);
      }

      @Override
      public void reset() {
        setComboBoxSelectedItem(comboBox, clientSetting::getValue);
      }
    };
  }

  static SelectionComponent<JComponent> diceRollerOverrideSelection() {
    final JTextField uriField = new JTextFieldBuilder().build();

    final JRadioButton production = new JRadioButton("Production (default)");
    final ActionListener disableCustomField =
        e -> {
          uriField.setText("");
          uriField.setEnabled(false);
        };
    production.addActionListener(disableCustomField);

    final JRadioButton custom = new JRadioButton("Custom");
    custom.addActionListener(e -> uriField.setEnabled(true));

    // set radio button selections
    production.setSelected(
        ClientSetting.diceRollerUri.getValueOrThrow().equals(DiceServerEditor.PRODUCTION_URI));
    custom.setSelected(!production.isSelected());

    uriField.setEnabled(custom.isSelected());
    uriField.setText(
        custom.isSelected() ? ClientSetting.diceRollerUri.getValueOrThrow().toString() : "");

    final ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.add(production);
    buttonGroup.add(custom);

    final var selectionPanel =
        new JPanelBuilder()
            .boxLayoutVertical()
            .add(new JPanelBuilder().add(production).add(custom).build())
            .add(uriField)
            .build();

    return new SelectionComponent<>() {
      @Override
      public JComponent getUiComponent() {
        return selectionPanel;
      }

      @Override
      public void save(final SaveContext context) {
        // if custom selected, read a value from the input field
        if (custom.isSelected()) {
          try {
            final URI uri = new URI(uriField.getText().trim());
            if (!uri.isAbsolute() || uriField.getText().isBlank()) {
              showInvalidUriError("Not a valid URI defined", uriField.getText());
            } else {
              context.setValue(ClientSetting.diceRollerUri, uri);
            }
          } catch (final URISyntaxException e) {
            showInvalidUriError(e.getMessage(), uriField.getText());
          }
        } else {
          context.setValue(ClientSetting.diceRollerUri, DiceServerEditor.PRODUCTION_URI);
        }
      }

      private void showInvalidUriError(final String errorMessage, final String fieldValue) {
        SwingComponents.showError(
            null, "Invalid URI", "Invalid URI, " + errorMessage + ": " + fieldValue);
      }

      @Override
      public void resetToDefault() {
        production.setSelected(true);
        uriField.setText("");
        uriField.setEnabled(false);
      }

      @Override
      public void reset() {
        production.setSelected(
            ClientSetting.diceRollerUri.getValueOrThrow().equals(DiceServerEditor.PRODUCTION_URI));
        custom.setSelected(!production.isSelected());
      }
    };
  }

  /**
   * Returns an unscrubbable string containing sensitive data. Use only when absolutely required.
   */
  private static String credentialToString(final Supplier<Optional<char[]>> credentialSupplier) {
    return withSensitiveArrayAndReturn(
        () -> credentialSupplier.get().orElseGet(() -> new char[0]), String::new);
  }

  static SelectionComponent<JComponent> emailSettings(
      final ClientSetting<String> hostSetting,
      final ClientSetting<Integer> portSetting,
      final ClientSetting<Boolean> tlsSetting,
      final ClientSetting<char[]> usernameSetting,
      final ClientSetting<char[]> passwordSetting) {
    return new SelectionComponent<>() {

      private final List<EmailProviderPreset> knownProviders =
          List.of(EmailProviderPreset.GMAIL, EmailProviderPreset.HOTMAIL);

      private final JTextField serverField = new JTextField(hostSetting.getValue().orElse(""), 20);

      private final JSpinner portSpinner =
          new JSpinner(
              new SpinnerNumberModel((int) portSetting.getValue().orElse(465), 0, 65535, 1));

      private final JCheckBox tlsCheckBox =
          new JCheckBox("SSL/TLS", tlsSetting.getValue().orElse(true));

      private final JTextField usernameField =
          new JTextField(credentialToString(usernameSetting::getValue), 20);

      private final JPasswordField passwordField =
          new JPasswordField(credentialToString(passwordSetting::getValue), 20);

      private final JPanel panel =
          new JPanelBuilder()
              .boxLayoutVertical()
              .addLeftJustified(new JLabel("Email Server"))
              .addLeftJustified(serverField)
              .addLeftJustified(new JLabel("Port"))
              .addLeftJustified(
                  new JPanelBuilder()
                      .boxLayoutHorizontal()
                      .addLeftJustified(portSpinner)
                      .addLeftJustified(tlsCheckBox)
                      .build())
              .addVerticalStrut(5)
              .addLeftJustified(
                  new JButtonBuilder()
                      .title("Presets...")
                      .actionListener(
                          () -> {
                            final List<String> selections =
                                knownProviders.stream()
                                    .map(EmailProviderPreset::getName)
                                    .collect(Collectors.toList());
                            final JComboBox<String> comboBox =
                                JComboBoxBuilder.builder().items(selections).build();
                            if (JOptionPane.showConfirmDialog(
                                    this.panel.getParent(),
                                    new JPanelBuilder().add(comboBox).build(),
                                    "Select a Preset",
                                    JOptionPane.OK_CANCEL_OPTION)
                                == JOptionPane.OK_OPTION) {

                              EmailProviderPreset.lookupByName((String) comboBox.getSelectedItem())
                                  .ifPresent(
                                      preset -> {
                                        ClientSetting.emailProvider.setValue(preset.getName());

                                        serverField.setText(preset.getServer());
                                        ClientSetting.emailServerHost.setValue(preset.getServer());

                                        portSpinner.setValue(preset.getPort());
                                        ClientSetting.emailServerPort.setValue(preset.getPort());

                                        tlsCheckBox.setSelected(preset.isUseTlsByDefault());
                                        ClientSetting.emailServerSecurity.setValueAndFlush(
                                            preset.isUseTlsByDefault());
                                      });
                            }
                          })
                      .build())
              .addLeftJustified(new JLabel("Username"))
              .addLeftJustified(usernameField)
              .addLeftJustified(new JLabel("Password"))
              .addLeftJustified(passwordField)
              .build();

      @Override
      public JComponent getUiComponent() {
        return panel;
      }

      @Override
      public void save(final SaveContext context) {
        context.setValue(hostSetting, Strings.emptyToNull(serverField.getText()));
        context.setValue(portSetting, (Integer) portSpinner.getValue());
        context.setValue(tlsSetting, tlsCheckBox.isSelected());
        final String username = usernameField.getText();
        context.setValue(usernameSetting, username.isEmpty() ? null : username.toCharArray());
        withSensitiveArray(
            passwordField::getPassword,
            password ->
                context.setValue(
                    passwordSetting,
                    (password.length == 0) ? null : password,
                    SaveContext.ValueSensitivity.SENSITIVE));
      }

      @Override
      public void resetToDefault() {
        serverField.setText(hostSetting.getDefaultValue().orElse(""));
        portSpinner.setValue(portSetting.getDefaultValue().orElse(465));
        tlsCheckBox.setSelected(tlsSetting.getDefaultValue().orElse(true));
        usernameField.setText(credentialToString(usernameSetting::getDefaultValue));
        passwordField.setText(credentialToString(passwordSetting::getDefaultValue));
      }

      @Override
      public void reset() {
        serverField.setText(hostSetting.getValue().orElse(""));
        portSpinner.setValue(portSetting.getValue().orElse(465));
        tlsCheckBox.setSelected(tlsSetting.getValue().orElse(true));
        usernameField.setText(credentialToString(usernameSetting::getValue));
        passwordField.setText(credentialToString(passwordSetting::getValue));
      }
    };
  }

  /**
   * Creates UI Controls to fetch a login token and safely store it in the ClientSettings.
   *
   * @param uidSetting The uid setting used to potentially revoke old tokens
   * @param usernameSetting The setting that stores the username being displayed to the user
   * @param tokenSetting The setting that stores the actual token.
   * @return A SelectionComponent that allows users to easily store their token.
   */
  static SelectionComponent<JComponent> forumPosterSettings(
      final String forumUrl,
      final ClientSetting<Integer> uidSetting,
      final ClientSetting<char[]> usernameSetting,
      final ClientSetting<char[]> tokenSetting) {
    return new SelectionComponent<>() {

      private final JTextField usernameField =
          new JTextFieldBuilder()
              .columns(20)
              .text(usernameSetting.getValue().map(String::new).orElse(""))
              .build();
      private final JPasswordField passwordField = new JPasswordField(20);
      private final JTextField otpField = new JTextField(20);

      private final JPanel mainPanel =
          new JPanelBuilder()
              .boxLayoutVertical()
              .addLeftJustified(new JLabel("Username:"))
              .addLeftJustified(usernameField)
              .addLeftJustified(new JLabel("Password:"))
              .addLeftJustified(passwordField)
              .addLeftJustified(new JLabel("2FA OTP Code (required for 2FA)"))
              .addLeftJustified(otpField)
              .build();

      @Override
      public JComponent getUiComponent() {
        return mainPanel;
      }

      @Override
      public void save(final SaveContext context) {
        // Only save when value changed
        if (usernameField
            .getText()
            .equals(usernameSetting.getValue().map(String::new).orElse(""))) {
          return;
        }
        BackgroundTaskRunner.runInBackground(
            "Fetching Login Token...",
            () -> {
              final NodeBbTokenGenerator tokenGenerator = new NodeBbTokenGenerator(forumUrl);
              final Optional<Integer> oldUserId = uidSetting.getValue();
              final Optional<char[]> oldToken = tokenSetting.getValue();
              if (!usernameField.getText().isBlank()) {
                final TokenInfo tokenInfo =
                    tokenGenerator.generateToken(
                        usernameField.getText(),
                        new String(passwordField.getPassword()),
                        Strings.emptyToNull(otpField.getText()));
                context.setValue(uidSetting, tokenInfo.getUserId());

                context.setValue(tokenSetting, tokenInfo.getToken().toCharArray());

                context.setValue(usernameSetting, usernameField.getText().toCharArray());
                // TODO error reporting
              } else {
                context.setValue(usernameSetting, null);
                context.setValue(uidSetting, null);
                context.setValue(tokenSetting, null);
              }

              oldUserId.ifPresent(
                  userId ->
                      oldToken.ifPresent(
                          token -> tokenGenerator.revokeToken(new String(token), userId)));
            });
      }

      @Override
      public void resetToDefault() {
        usernameField.setText("");
        passwordField.setText("");
      }

      @Override
      public void reset() {
        usernameField.setText(usernameSetting.getValue().map(String::new).orElse(""));
        passwordField.setText("");
      }
    };
  }
}
