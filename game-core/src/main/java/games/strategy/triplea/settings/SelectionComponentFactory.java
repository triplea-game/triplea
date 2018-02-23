package games.strategy.triplea.settings;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.google.common.base.Strings;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.ui.SwingComponents;
import swinglib.JButtonBuilder;
import swinglib.JPanelBuilder;

/**
 * Logic for building UI components that "bind" to ClientSettings.
 * For example, if we have a setting that needs a number, we could create an integer text field with this
 * class. This class takes care of the UI code to ensure we render the proper swing component with validation.
 */
final class SelectionComponentFactory {
  private SelectionComponentFactory() {}

  static Supplier<SelectionComponent<JComponent>> proxySettings() {
    return () -> new SelectionComponent<JComponent>() {
      final Preferences pref = Preferences.userNodeForPackage(GameRunner.class);
      final HttpProxy.ProxyChoice proxyChoice =
          HttpProxy.ProxyChoice.valueOf(pref.get(HttpProxy.PROXY_CHOICE, HttpProxy.ProxyChoice.NONE.toString()));
      final JRadioButton noneButton = new JRadioButton("None", proxyChoice == HttpProxy.ProxyChoice.NONE);
      final JRadioButton systemButton =
          new JRadioButton("Use System Settings", proxyChoice == HttpProxy.ProxyChoice.USE_SYSTEM_SETTINGS);


      final JRadioButton userButton =
          new JRadioButton("Use These Settings:", proxyChoice == HttpProxy.ProxyChoice.USE_USER_PREFERENCES);
      final JTextField hostText = new JTextField(ClientSetting.PROXY_HOST.value(), 20);
      final JTextField portText = new JTextField(ClientSetting.PROXY_PORT.value(), 6);
      final JPanel radioPanel = JPanelBuilder.builder()
          .verticalBoxLayout()
          .add(noneButton)
          .add(systemButton)
          .add(userButton)
          .add(new JLabel("Proxy Host: "))
          .add(hostText)
          .add(new JLabel("Proxy Port: "))
          .add(portText)
          .build();

      final ActionListener enableUserSettings = e -> {
        if (userButton.isSelected()) {
          hostText.setEnabled(true);
          hostText.setBackground(Color.WHITE);
          portText.setEnabled(true);
          portText.setBackground(Color.WHITE);
        } else {
          hostText.setEnabled(false);
          hostText.setBackground(Color.DARK_GRAY);
          portText.setEnabled(false);
          portText.setBackground(Color.DARK_GRAY);
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
      public Map<GameSetting, String> readValues() {
        final Map<GameSetting, String> values = new HashMap<>();
        if (noneButton.isSelected()) {
          values.put(ClientSetting.PROXY_CHOICE, HttpProxy.ProxyChoice.NONE.toString());
        } else if (systemButton.isSelected()) {
          values.put(ClientSetting.PROXY_CHOICE, HttpProxy.ProxyChoice.USE_SYSTEM_SETTINGS.toString());
          HttpProxy.updateSystemProxy();
        } else {
          values.put(ClientSetting.PROXY_CHOICE, HttpProxy.ProxyChoice.USE_USER_PREFERENCES.toString());
          values.put(ClientSetting.PROXY_HOST, hostText.getText().trim());
          values.put(ClientSetting.PROXY_PORT, portText.getText().trim());
        }
        return values;
      }

      @Override
      public void indicateError() {
        if (!isHostTextValid()) {
          hostText.setBackground(Color.RED);
        }
        if (!isPortTextValid()) {
          portText.setBackground(Color.RED);
        }
      }

      @Override
      public void clearError() {
        hostText.setBackground(Color.WHITE);
        portText.setBackground(Color.WHITE);
      }

      @Override
      public void resetToDefault() {
        ClientSetting.flush();
        hostText.setText(ClientSetting.PROXY_HOST.defaultValue);
        portText.setText(ClientSetting.PROXY_PORT.defaultValue);
        noneButton.setSelected(Boolean.valueOf(ClientSetting.PROXY_CHOICE.defaultValue));
      }

      @Override
      public void reset() {
        ClientSetting.flush();
        hostText.setText(ClientSetting.PROXY_HOST.value());
        portText.setText(ClientSetting.PROXY_PORT.value());
        noneButton.setSelected(ClientSetting.PROXY_CHOICE.booleanValue());
      }
    };
  }

  /**
   * Text field that only accepts numbers between a certain range.
   */
  static Supplier<SelectionComponent<JComponent>> intValueRange(final ClientSetting clientSetting, final int lo,
      final int hi) {
    return intValueRange(clientSetting, lo, hi, false);
  }

  /**
   * Text field that only accepts numbers between a certain range.
   */
  static Supplier<SelectionComponent<JComponent>> intValueRange(final ClientSetting clientSetting, final int lo,
      final int hi, final boolean allowUnset) {
    return () -> new SelectionComponent<JComponent>() {
      private final JSpinner component = new JSpinner(new SpinnerNumberModel(
          toValidIntValue(clientSetting.value()), lo - (allowUnset ? 1 : 0), hi, 1));

      @Override
      public JComponent getUiComponent() {
        return component;
      }

      private int toValidIntValue(final String value) {
        return (value.isEmpty() && allowUnset) ? (lo - 1) : Integer.parseInt(value);
      }

      private String toValidStringValue(final int value) {
        return (allowUnset && (value == (lo - 1))) ? "" : String.valueOf(value);
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
      public void indicateError() {}

      @Override
      public void clearError() {}

      @Override
      public Map<GameSetting, String> readValues() {
        return Collections.singletonMap(clientSetting, toValidStringValue((int) component.getValue()));
      }

      @Override
      public void resetToDefault() {
        component.setValue(toValidIntValue(clientSetting.defaultValue));
      }

      @Override
      public void reset() {
        component.setValue(toValidIntValue(clientSetting.value()));
      }
    };
  }

  /**
   * yes/no radio buttons.
   */
  static SelectionComponent<JComponent> booleanRadioButtons(final ClientSetting clientSetting) {
    return new AlwaysValidInputSelectionComponent() {
      final boolean initialSelection = clientSetting.booleanValue();
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
      public Map<GameSetting, String> readValues() {
        final String value = yesButton.isSelected() ? String.valueOf(true) : String.valueOf(false);
        final Map<GameSetting, String> settingMap = new HashMap<>();
        settingMap.put(clientSetting, value);
        return settingMap;
      }

      @Override
      public void resetToDefault() {
        yesButton.setSelected(Boolean.valueOf(clientSetting.defaultValue));
        noButton.setSelected(!Boolean.valueOf(clientSetting.defaultValue));
      }

      @Override
      public void reset() {
        yesButton.setSelected(clientSetting.booleanValue());
        noButton.setSelected(!clientSetting.booleanValue());
      }
    };
  }

  /**
   * File selection prompt.
   */
  static Supplier<SelectionComponent<JComponent>> filePath(final ClientSetting clientSetting) {
    return selectFile(clientSetting, SwingComponents.FolderSelectionMode.FILES);
  }

  private static Supplier<SelectionComponent<JComponent>> selectFile(
      final ClientSetting clientSetting,
      final SwingComponents.FolderSelectionMode folderSelectionMode) {
    return () -> new AlwaysValidInputSelectionComponent() {
      final int expectedLength = 20;
      final JTextField field = new JTextField(clientSetting.value(), expectedLength);
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
      public Map<GameSetting, String> readValues() {
        final String value = field.getText();
        final Map<GameSetting, String> settingMap = new HashMap<>();
        settingMap.put(clientSetting, value);
        return settingMap;
      }

      @Override
      public void resetToDefault() {
        field.setText(clientSetting.defaultValue);
        clearError();
      }

      @Override
      public void reset() {
        field.setText(clientSetting.value());
        clearError();
      }
    };
  }

  /**
   * Folder selection prompt.
   */
  static Supplier<SelectionComponent<JComponent>> folderPath(final ClientSetting clientSetting) {
    return selectFile(clientSetting, SwingComponents.FolderSelectionMode.DIRECTORIES);
  }


  static <T> Supplier<SelectionComponent<JComponent>> selectionBox(
      final ClientSetting clientSetting,
      final List<T> availableOptions,
      final Function<T, ?> renderFunction) {
    return () -> new AlwaysValidInputSelectionComponent() {
      final JComboBox<T> comboBox = getCombobox();

      private JComboBox<T> getCombobox() {
        final JComboBox<T> comboBox = new JComboBox<>();
        availableOptions.forEach(comboBox::addItem);
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
        comboBox.setSelectedItem(clientSetting.value());
        return comboBox;
      }

      @Override
      public Map<GameSetting, String> readValues() {
        final String value = String.valueOf(comboBox.getSelectedItem());
        final Map<GameSetting, String> settingMap = new HashMap<>();
        settingMap.put(clientSetting, value);
        return settingMap;
      }

      @Override
      public void resetToDefault() {
        comboBox.setSelectedItem(clientSetting.defaultValue);
        clearError();
      }

      @Override
      public void reset() {
        comboBox.setSelectedItem(clientSetting.value());
        clearError();
      }
    };
  }

  static Supplier<SelectionComponent<JComponent>> textField(final ClientSetting clientSetting) {
    return () -> new AlwaysValidInputSelectionComponent() {
      final JTextField textField = new JTextField(clientSetting.value(), 20);

      @Override
      public JComponent getUiComponent() {
        return textField;
      }

      @Override
      public Map<GameSetting, String> readValues() {
        final Map<GameSetting, String> map = new HashMap<>();
        map.put(clientSetting, textField.getText());
        return map;
      }

      @Override
      public void reset() {
        textField.setText(clientSetting.value());
        clearError();
      }

      @Override
      public void resetToDefault() {
        textField.setText(clientSetting.defaultValue);
        clearError();
      }
    };
  }

  private abstract static class AlwaysValidInputSelectionComponent implements SelectionComponent<JComponent> {
    @Override
    public void indicateError() {
      // no-op, component only allows valid selections
    }

    @Override
    public void clearError() {
      // also a no-op
    }

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
