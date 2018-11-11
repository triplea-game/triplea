package games.strategy.triplea.settings;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.ui.SwingComponents;
import lombok.AllArgsConstructor;
import lombok.Getter;
import swinglib.JButtonBuilder;
import swinglib.JComboBoxBuilder;
import swinglib.JPanelBuilder;

/**
 * Logic for building UI components that "bind" to ClientSettings.
 * For example, if we have a setting that needs a number, we could create an integer text field with this
 * class. This class takes care of the UI code to ensure we render the proper swing component with validation.
 */
final class SelectionComponentFactory {
  private SelectionComponentFactory() {}

  static SelectionComponent<JComponent> proxySettings(
      final ClientSetting<HttpProxy.ProxyChoice> proxyChoiceClientSetting,
      final ClientSetting<String> proxyHostClientSetting,
      final ClientSetting<Integer> proxyPortClientSetting) {
    return new SelectionComponent<JComponent>() {
      final HttpProxy.ProxyChoice proxyChoice = proxyChoiceClientSetting.getValueOrThrow();
      final JRadioButton noneButton = new JRadioButton("None", proxyChoice == HttpProxy.ProxyChoice.NONE);
      final JRadioButton systemButton =
          new JRadioButton("Use System Settings", proxyChoice == HttpProxy.ProxyChoice.USE_SYSTEM_SETTINGS);
      final JRadioButton userButton =
          new JRadioButton("Use These Settings:", proxyChoice == HttpProxy.ProxyChoice.USE_USER_PREFERENCES);
      final JTextField hostText = new JTextField(proxyHostClientSetting.getValue().orElse(""), 20);
      final JTextField portText = new JTextField(proxyPortClientSetting.getValue().map(Object::toString).orElse(""), 6);
      final JPanel radioPanel = JPanelBuilder.builder()
          .verticalBoxLayout()
          .addLeftJustified(noneButton)
          .addLeftJustified(systemButton)
          .addLeftJustified(userButton)
          .addLeftJustified(JPanelBuilder.builder()
              .horizontalBoxLayout()
              .addHorizontalStrut(getRadioButtonLabelHorizontalOffset())
              .add(JPanelBuilder.builder()
                  .verticalBoxLayout()
                  .addLeftJustified(new JLabel("Proxy Host:"))
                  .addLeftJustified(hostText)
                  .addLeftJustified(new JLabel("Proxy Port:"))
                  .addLeftJustified(portText)
                  .build())
              .build())
          .build();
      final ActionListener enableUserSettings = e -> {
        if (userButton.isSelected()) {
          hostText.setEnabled(true);
          portText.setEnabled(true);
        } else {
          hostText.setEnabled(false);
          portText.setEnabled(false);
        }
      };

      @Override
      public JComponent getUiComponent() {
        SwingComponents.createButtonGroup(noneButton, systemButton, userButton);
        enableUserSettings.actionPerformed(null);
        userButton.addActionListener(enableUserSettings);
        noneButton.addActionListener(enableUserSettings);
        systemButton.addActionListener(enableUserSettings);

        return radioPanel;
      }

      @Override
      public boolean isValid() {
        return !userButton.isSelected() || (isHostTextValid() && isPortTextValid());
      }

      private boolean isHostTextValid() {
        return !Strings.nullToEmpty(hostText.getText()).trim().isEmpty();
      }

      private boolean isPortTextValid() {
        final String value = Strings.nullToEmpty(portText.getText()).trim();
        if (value.isEmpty()) {
          return false;
        }

        try {
          return Integer.parseInt(value) > 0;
        } catch (final NumberFormatException e) {
          return false;
        }
      }

      @Override
      public String validValueDescription() {
        return "Proxy host can be a network name or an IP address, port should be number, usually 4 to 5 digits.";
      }

      @Override
      public Map<GameSetting<?>, Object> readValues() {
        final Map<GameSetting<?>, Object> values = new HashMap<>();

        if (noneButton.isSelected()) {
          values.put(proxyChoiceClientSetting, HttpProxy.ProxyChoice.NONE);
        } else if (systemButton.isSelected()) {
          values.put(proxyChoiceClientSetting, HttpProxy.ProxyChoice.USE_SYSTEM_SETTINGS);
          HttpProxy.updateSystemProxy();
        } else {
          values.put(proxyChoiceClientSetting, HttpProxy.ProxyChoice.USE_USER_PREFERENCES);
        }

        final String host = hostText.getText().trim();
        values.put(proxyHostClientSetting, host.isEmpty() ? null : host);

        final String encodedPort = portText.getText().trim();
        values.put(proxyPortClientSetting, encodedPort.isEmpty() ? null : Integer.valueOf(encodedPort));

        return values;
      }

      @Override
      public void resetToDefault() {
        ClientSetting.flush();
        hostText.setText(proxyHostClientSetting.getDefaultValue().orElse(""));
        portText.setText(proxyPortClientSetting.getDefaultValue().map(Object::toString).orElse(""));
        setProxyChoice(proxyChoiceClientSetting.getDefaultValue().orElse(HttpProxy.ProxyChoice.NONE));
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

  /**
   * Text field that only accepts numbers between a certain range.
   */
  static SelectionComponent<JComponent> intValueRange(
      final ClientSetting<Integer> clientSetting,
      final int lo,
      final int hi) {
    return intValueRange(clientSetting, lo, hi, false);
  }

  /**
   * Text field that only accepts numbers between a certain range.
   */
  static SelectionComponent<JComponent> intValueRange(
      final ClientSetting<Integer> clientSetting,
      final int lo,
      final int hi,
      final boolean allowUnset) {
    return new SelectionComponent<JComponent>() {
      private final JSpinner component = new JSpinner(new SpinnerNumberModel(
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
      public boolean isValid() {
        return true;
      }

      @Override
      public String validValueDescription() {
        return "";
      }

      @Override
      public Map<GameSetting<?>, Object> readValues() {
        return Collections.singletonMap(clientSetting, getComponentValue());
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

  /**
   * yes/no radio buttons.
   */
  static SelectionComponent<JComponent> booleanRadioButtons(final ClientSetting<Boolean> clientSetting) {
    return new AlwaysValidInputSelectionComponent() {
      final boolean initialSelection = clientSetting.getValueOrThrow();
      final JRadioButton yesButton = new JRadioButton("True");
      final JRadioButton noButton = new JRadioButton("False");
      final JPanel buttonPanel = JPanelBuilder.builder()
          .horizontalBoxLayout()
          .add(yesButton)
          .add(noButton)
          .build();

      @Override
      public JComponent getUiComponent() {
        yesButton.setSelected(initialSelection);
        noButton.setSelected(!initialSelection);
        SwingComponents.createButtonGroup(yesButton, noButton);
        return buttonPanel;
      }

      @Override
      public Map<GameSetting<?>, Object> readValues() {
        return Collections.singletonMap(clientSetting, yesButton.isSelected());
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

  /**
   * File selection prompt.
   */
  static SelectionComponent<JComponent> filePath(final ClientSetting<Path> clientSetting) {
    return selectFile(clientSetting, SwingComponents.FolderSelectionMode.FILES);
  }

  private static SelectionComponent<JComponent> selectFile(
      final ClientSetting<Path> clientSetting,
      final SwingComponents.FolderSelectionMode folderSelectionMode) {
    return new AlwaysValidInputSelectionComponent() {
      final JTextField field = new JTextField(SelectionComponentUiUtils.toString(clientSetting.getValue()), 20);
      final JButton button = JButtonBuilder.builder()
          .title("Select")
          .actionListener(
              () -> SwingComponents.showJFileChooser(folderSelectionMode)
                  .ifPresent(file -> field.setText(file.getAbsolutePath())))
          .build();

      @Override
      public JComponent getUiComponent() {
        field.setEditable(false);

        return JPanelBuilder.builder()
            .horizontalBoxLayout()
            .add(field)
            .addHorizontalStrut(5)
            .add(button)
            .build();
      }

      @Override
      public Map<GameSetting<?>, Object> readValues() {
        final String value = field.getText();
        return Collections.singletonMap(clientSetting, value.isEmpty() ? null : Paths.get(value));
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

  /**
   * Folder selection prompt.
   */
  static SelectionComponent<JComponent> folderPath(final ClientSetting<Path> clientSetting) {
    return selectFile(clientSetting, SwingComponents.FolderSelectionMode.DIRECTORIES);
  }

  static <T> SelectionComponent<JComponent> selectionBox(
      final ClientSetting<String> clientSetting,
      final List<T> availableOptions,
      final T selectedOption,
      final Function<T, ?> renderFunction) {
    return new AlwaysValidInputSelectionComponent() {
      final JComboBox<T> comboBox = getCombobox();

      private JComboBox<T> getCombobox() {
        final JComboBox<T> comboBox = new JComboBox<>();
        availableOptions.forEach(comboBox::addItem);
        comboBox.setSelectedItem(selectedOption);
        comboBox.setRenderer(new DefaultListCellRenderer() {
          private static final long serialVersionUID = -3094995494539073655L;

          @Override
          @SuppressWarnings("unchecked")
          public Component getListCellRendererComponent(
              final JList<?> list, final Object value, final int index, final boolean isSelected,
              final boolean cellHasFocus) {
            return super.getListCellRendererComponent(list, renderFunction.apply((T) value), index, isSelected,
                cellHasFocus);
          }
        });
        return comboBox;
      }

      @Override
      public JComponent getUiComponent() {
        comboBox.setSelectedItem(clientSetting.getValue().orElse(null));
        return comboBox;
      }

      @Override
      public Map<GameSetting<?>, Object> readValues() {
        return Collections.singletonMap(clientSetting, Objects.toString(comboBox.getSelectedItem(), null));
      }

      @Override
      public void resetToDefault() {
        comboBox.setSelectedItem(clientSetting.getDefaultValue().orElse(null));
      }

      @Override
      public void reset() {
        comboBox.setSelectedItem(clientSetting.getValue().orElse(null));
      }
    };
  }

  static SelectionComponent<JComponent> textField(final ClientSetting<String> clientSetting) {
    return new AlwaysValidInputSelectionComponent() {
      final JTextField textField = new JTextField(clientSetting.getValue().orElse(""), 20);

      @Override
      public JComponent getUiComponent() {
        return textField;
      }

      @Override
      public Map<GameSetting<?>, Object> readValues() {
        final String value = textField.getText();
        return Collections.singletonMap(clientSetting, value.isEmpty() ? null : value);
      }

      @Override
      public void reset() {
        textField.setText(clientSetting.getValue().orElse(""));
      }

      @Override
      public void resetToDefault() {
        textField.setText(clientSetting.getDefaultValue().orElse(""));
      }
    };
  }

  static SelectionComponent<JComponent> emailSettings(
      final ClientSetting<String> hostSetting,
      final ClientSetting<Integer> portSetting,
      final ClientSetting<Boolean> tlsSetting,
      final ClientSetting<String> usernameSetting,
      final ClientSetting<String> passwordSetting) {
    return new AlwaysValidInputSelectionComponent() {


      /**
       * Data class to store a 3-tuple consisting of
       * a server host, a server port and whether or not
       * to use an encrypted connection.
       */
      @AllArgsConstructor
      @Immutable
      final class EmailProviderSetting {
        @Nonnull
        private final String displayName;
        @Getter
        @Nonnull
        private final String host;
        @Getter
        @Nonnull
        private final int port;
        @Getter
        @Nonnull
        private final boolean isEncrypted;

        @Override
        public String toString() {
          return displayName;
        }
      }

      private final List<EmailProviderSetting> knownProviders = Arrays.asList(
          new EmailProviderSetting("GMail", "smtp.gmail.com", 587, true),
          new EmailProviderSetting("Hotmail", "smtp.live.com", 587, true)
      );

      final JTextField serverField = new JTextField(hostSetting.getValue().orElse(""), 20);

      final JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(
          (int) portSetting.getValue().orElse(0), 0, 65535, 1));

      final JCheckBox tlsCheckBox = new JCheckBox("SSL/TLS", tlsSetting.getValue().orElse(true));

      final JTextField usernameField = new JTextField(usernameSetting.getValue().orElse(""), 20);

      final JPasswordField passwordField = new JPasswordField(passwordSetting.getValue().orElse(""), 20);

      final JPanel panel = JPanelBuilder.builder()
          .verticalBoxLayout()
          .addLeftJustified(new JLabel("Email Server"))
          .addLeftJustified(serverField)
          .addLeftJustified(new JLabel("Port"))
          .addLeftJustified(JPanelBuilder.builder()
              .horizontalBoxLayout()
              .addLeftJustified(portSpinner)
              .addLeftJustified(tlsCheckBox)
          .build())
          .addVerticalStrut(5)
          .addLeftJustified(JButtonBuilder.builder()
              .title("Presets...")
              .actionListener(() -> {
                final JComboBox<EmailProviderSetting> comboBox =
                    JComboBoxBuilder.builder(EmailProviderSetting.class)
                        .items(knownProviders)
                        .build();
                if (JOptionPane.showConfirmDialog(this.panel.getParent(), JPanelBuilder.builder().add(comboBox).build(),
                    "Select a Preset", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                  final EmailProviderSetting config = (EmailProviderSetting) comboBox.getSelectedItem();
                  serverField.setText(config.getHost());
                  portSpinner.setValue(config.getPort());
                  tlsCheckBox.setSelected(config.isEncrypted());
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
      public Map<GameSetting<?>, Object> readValues() {
        final char[] password = passwordField.getPassword();

        final Map<GameSetting<?>, Object> map = ImmutableMap.<GameSetting<?>, Object>builder()
            .put(hostSetting, Strings.emptyToNull(serverField.getText()))
            .put(portSetting, portSpinner.getValue())
            .put(tlsSetting, tlsCheckBox.isSelected())
            .put(usernameSetting, Strings.emptyToNull(usernameField.getText()))
            .put(passwordSetting, password.length == 0 ? null : new String(password))
            .build();
        Arrays.fill(password, '\0');
        return map;
      }

      @Override
      public void resetToDefault() {
        serverField.setText(hostSetting.getDefaultValue().orElse(""));
        portSpinner.setValue(portSetting.getDefaultValue().orElse(0));
        tlsCheckBox.setSelected(tlsSetting.getDefaultValue().orElse(true));
        usernameField.setText(usernameSetting.getDefaultValue().orElse(""));
        passwordField.setText(passwordSetting.getDefaultValue().orElse(""));
      }

      @Override
      public void reset() {
        serverField.setText(hostSetting.getValue().orElse(""));
        portSpinner.setValue(portSetting.getValue().orElse(0));
        tlsCheckBox.setSelected(tlsSetting.getValue().orElse(true));
        usernameField.setText(usernameSetting.getValue().orElse(""));
        passwordField.setText(passwordSetting.getValue().orElse(""));
      }
    };
  }

  private abstract static class AlwaysValidInputSelectionComponent implements SelectionComponent<JComponent> {
    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public String validValueDescription() {
      return "";
    }
  }
}
