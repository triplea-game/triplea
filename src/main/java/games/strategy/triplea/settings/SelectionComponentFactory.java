package games.strategy.triplea.settings;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import com.google.common.base.Strings;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.ui.SwingComponents;

/**
 * Logic for building UI components that "bind" to ClientSettings.
 * For example, if we have a setting that needs a number, we could create an integer text field with this
 * class. This class takes care of the UI code to ensure we render the proper swing component with validation.
 */
class SelectionComponentFactory {
  static SelectionComponent proxySettings() {
    final Preferences pref = Preferences.userNodeForPackage(GameRunner.class);


    final HttpProxy.ProxyChoice proxyChoice =
        HttpProxy.ProxyChoice.valueOf(pref.get(HttpProxy.PROXY_CHOICE, HttpProxy.ProxyChoice.NONE.toString()));

    final JRadioButton noneButton = new JRadioButton("None", proxyChoice == HttpProxy.ProxyChoice.NONE);

    final JRadioButton systemButton =
        new JRadioButton("Use System Settings", proxyChoice == HttpProxy.ProxyChoice.USE_SYSTEM_SETTINGS);

    final JRadioButton userButton =
        new JRadioButton("Use These Settings:", proxyChoice == HttpProxy.ProxyChoice.USE_USER_PREFERENCES);



    SwingComponents.createButtonGroup(noneButton, systemButton, userButton);


    final JPanel radioPanel = SwingComponents.newJPanelWithVerticalBoxLayout();
    radioPanel.add(noneButton);
    radioPanel.add(systemButton);
    radioPanel.add(userButton);


    final JTextField hostText = new JTextField(ClientSetting.PROXY_HOST.value(), 20);
    radioPanel.add(new JLabel("Proxy Host: "));
    radioPanel.add(hostText);

    final JTextField portText = new JTextField(ClientSetting.PROXY_PORT.value(), 6);
    radioPanel.add(new JLabel("Proxy Port: "));
    radioPanel.add(portText);

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
    enableUserSettings.actionPerformed(null);
    userButton.addActionListener(enableUserSettings);
    noneButton.addActionListener(enableUserSettings);
    systemButton.addActionListener(enableUserSettings);

    return new SelectionComponent() {

      private static final long serialVersionUID = -8485825527073729683L;

      @Override
      JComponent getJComponent() {
        return radioPanel;
      }

      @Override
      boolean isValid() {
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
      String validValueDescription() {
        return "Proxy host can be a network name or an IP address, port should be number, usually 4 to 5 digits.";
      }

      @Override
      Map<ClientSetting, String> readValues() {
        final Map<ClientSetting, String> values = new HashMap<>();
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
      void indicateError() {
        if (!isHostTextValid()) {
          hostText.setBackground(Color.RED);
        }
        if (!isPortTextValid()) {
          portText.setBackground(Color.RED);
        }
      }

      @Override
      void clearError() {
        hostText.setBackground(Color.WHITE);
        portText.setBackground(Color.WHITE);

      }

      @Override
      void resetToDefault() {
        ClientSetting.PROXY_CHOICE.restoreToDefaultValue();
        ClientSetting.PROXY_HOST.restoreToDefaultValue();
        ClientSetting.PROXY_PORT.restoreToDefaultValue();
        ClientSetting.flush();
        hostText.setText("");
        portText.setText("");
        noneButton.setSelected(true);
      }
    };
  }

  /**
   * Text field that only accepts numbers between a certain range.
   */
  static SelectionComponent intValueRange(final ClientSetting clientSetting, final int lo, final int hi) {
    final JTextField component = new JTextField(clientSetting.value(), String.valueOf(hi).length());

    return new SelectionComponent() {
      private static final long serialVersionUID = 8195633990481917808L;

      @Override
      JComponent getJComponent() {
        component.setToolTipText(validValueDescription());

        SwingComponents.addTextFieldFocusLostListener(component, () -> {
          if (isValid()) {
            clearError();
          } else {
            indicateError();
          }
        });

        return component;
      }

      @Override
      boolean isValid() {
        final String value = component.getText();

        try {
          final int intValue = Integer.parseInt(value);
          return intValue >= lo && intValue <= hi;
        } catch (final NumberFormatException e) {
          return false;
        }
      }

      @Override
      String validValueDescription() {
        return "Number between " + lo + " and " + hi;
      }

      @Override
      void indicateError() {
        component.setBackground(Color.RED);
      }

      @Override
      void clearError() {
        component.setBackground(Color.WHITE);
      }

      @Override
      Map<ClientSetting, String> readValues() {
        final Map<ClientSetting, String> map = new HashMap<>();
        map.put(clientSetting, component.getText());
        return map;
      }

      @Override
      void resetToDefault() {
        clientSetting.restoreToDefaultValue();
        ClientSetting.flush();
        component.setText(clientSetting.value());
      }
    };
  }

  /**
   * yes/no radio buttons.
   */
  static SelectionComponent booleanRadioButtons(final ClientSetting clientSetting) {
    final boolean initialSelection = clientSetting.booleanValue();

    final JRadioButton yesButton = new JRadioButton("True");
    yesButton.setSelected(initialSelection);

    final JRadioButton noButton = new JRadioButton("False");
    noButton.setSelected(!initialSelection);

    SwingComponents.createButtonGroup(yesButton, noButton);

    final JPanel buttonPanel = SwingComponents.newJPanelWithHorizontalBoxLayout();
    buttonPanel.add(yesButton);
    buttonPanel.add(noButton);

    return new AlwaysValidInputSelectionComponent(clientSetting) {
      private static final long serialVersionUID = 6104513062312556269L;

      @Override
      JComponent getJComponent() {
        return buttonPanel;
      }

      @Override
      Map<ClientSetting, String> readValues() {
        final String value = yesButton.isSelected() ? String.valueOf(true) : String.valueOf(false);
        final Map<ClientSetting, String> settingMap = new HashMap<>();
        settingMap.put(clientSetting, value);
        return settingMap;
      }
    };
  }

  /**
   * Folder selection prompt, returns nothing when user cancels or closes window.
   */
  static SelectionComponent folderPath(final ClientSetting clientSetting) {
    final int expectedLength = 20;
    final JTextField field = new JTextField(clientSetting.value(), expectedLength);
    field.setEditable(false);

    final JButton button = SwingComponents.newJButton(
        "Select",
        action -> SwingComponents.showJFileChooserForFolders()
            .ifPresent(file -> field.setText(file.getAbsolutePath())));

    return new AlwaysValidInputSelectionComponent(clientSetting) {
      private static final long serialVersionUID = -1775099967925891332L;

      @Override
      JComponent getJComponent() {
        final JPanel panel = SwingComponents.newJPanelWithHorizontalBoxLayout();
        panel.add(field);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(button);
        return panel;
      }

      @Override
      Map<ClientSetting, String> readValues() {
        final String value = field.getText();
        final Map<ClientSetting, String> settingMap = new HashMap<>();
        settingMap.put(clientSetting, value);
        return settingMap;
      }
    };
  }


  static SelectionComponent selectionBox(final ClientSetting clientSetting, final List<String> availableOptions) {
    final JComboBox<String> comboBox = new JComboBox<>(availableOptions.toArray(new String[availableOptions.size()]));

    return new AlwaysValidInputSelectionComponent(clientSetting) {
      private static final long serialVersionUID = -8969206423938554118L;

      @Override
      JComponent getJComponent() {
        return comboBox;
      }

      @Override
      Map<ClientSetting, String> readValues() {
        final String value = String.valueOf(comboBox.getSelectedItem());
        final Map<ClientSetting, String> settingMap = new HashMap<>();
        settingMap.put(clientSetting, value);
        return settingMap;
      }
    };
  }

  private abstract static class AlwaysValidInputSelectionComponent extends SelectionComponent {
    private static final long serialVersionUID = 6848335387637901069L;

    private final ClientSetting clientSetting;

    AlwaysValidInputSelectionComponent(final ClientSetting clientSetting) {
      this.clientSetting = clientSetting;
    }

    @Override
    void indicateError() {
      // no-op, component only allows valid selections
    }

    @Override
    void clearError() {
      // also a no-op
    }

    @Override
    boolean isValid() {
      return true;
    }

    @Override
    String validValueDescription() {
      return "";
    }

    @Override
    void resetToDefault() {
      clientSetting.restoreToDefaultValue();
    }
  }

}
