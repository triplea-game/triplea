package org.triplea.client.ui.javafx;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

import com.google.common.base.Strings;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.settings.GameSetting;
import games.strategy.triplea.settings.SelectionComponent;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

class JavaFxSelectionComponentFactory {

  private JavaFxSelectionComponentFactory() {}

  static Supplier<SelectionComponent<Node>> intValueRange(
      final ClientSetting clientSetting,
      final int minValue,
      final int maxValue) {
    return () -> {
      final Spinner<Integer> spinner = new Spinner<>(minValue, maxValue, clientSetting.intValue());
      return new SelectionComponent<Node>() {

        @Override
        public Node getJComponent() {
          return spinner;
        }

        @Override
        public boolean isValid() {
          return true;
        }

        @Override
        public String validValueDescription() {
          return "";// TODO localize this as well
        }

        @Override
        public Map<GameSetting, String> readValues() {
          return Collections.singletonMap(clientSetting, spinner.getValue().toString());
        }

        /**
         * Does nothing.
         * Using a Spinner should ensure no invalid values can be entered.
         */
        @Override
        public void indicateError() {}

        /**
         * Does nothing.
         * Using a Spinner should ensure no invalid values can be entered.
         */
        @Override
        public void clearError() {}

        @Override
        public void resetToDefault() {
          spinner.getValueFactory().setValue(Integer.getInteger(clientSetting.defaultValue));
        }

        @Override
        public void reset() {
          spinner.getValueFactory().setValue(Integer.getInteger(clientSetting.value()));
        }

        @Override
        public String getTitle() {
          return "";
        }
      };
    };
  }

  static Supplier<SelectionComponent<Node>> selectionBox(final ClientSetting clientSetting, final List<String> values) {
    return () -> {
      final ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableList(values));
      return new SelectionComponent<Node>() {

        @Override
        public Node getJComponent() {
          return comboBox;
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
        public Map<GameSetting, String> readValues() {
          return Collections.singletonMap(clientSetting, String.valueOf(comboBox.getValue()));
        }

        @Override
        public void indicateError() {}

        @Override
        public void clearError() {}

        @Override
        public void resetToDefault() {
          comboBox.setValue(clientSetting.defaultValue);
        }

        @Override
        public void reset() {
          comboBox.setValue(clientSetting.value());
        }

      };
    };
  }

  static Supplier<SelectionComponent<Node>> toggleButton(final ClientSetting clientSetting) {
    return () -> {
      final CheckBox checkBox = new CheckBox();
      checkBox.setSelected(Boolean.parseBoolean(clientSetting.value()));
      return new SelectionComponent<Node>() {

        @Override
        public Node getJComponent() {
          return checkBox;
        }

        @Override
        public boolean isValid() {
          return true;
        }

        @Override
        public String validValueDescription() {
          return "";// TODO localize
        }

        @Override
        public Map<GameSetting, String> readValues() {
          return Collections.singletonMap(clientSetting, String.valueOf(checkBox.isSelected()));
        }

        @Override
        public void indicateError() {}

        @Override
        public void clearError() {}

        @Override
        public void resetToDefault() {
          checkBox.setSelected(Boolean.parseBoolean(clientSetting.defaultValue));
        }

        @Override
        public void reset() {
          checkBox.setSelected(Boolean.parseBoolean(clientSetting.value()));
        }
      };
    };
  }

  static Supplier<SelectionComponent<Node>> textField(final ClientSetting clientSetting) {
    return () -> {
      final TextField textField = new TextField();
      return new SelectionComponent<Node>() {

        @Override
        public Node getJComponent() {
          return textField;
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
        public Map<GameSetting, String> readValues() {
          return Collections.singletonMap(clientSetting, textField.getText());
        }

        @Override
        public void indicateError() {}

        @Override
        public void clearError() {}

        @Override
        public void resetToDefault() {
          textField.setText(clientSetting.defaultValue);
        }

        @Override
        public void reset() {
          textField.setText(clientSetting.value());
        }
      };
    };
  }


  static Supplier<SelectionComponent<Node>> folderPath(final ClientSetting clientSetting) {
    return () -> {
      final HBox wrapper = new HBox();
      final TextField textField = new TextField(clientSetting.value());
      textField.setDisable(true);
      final Button chooseFileButton = new Button();// Localize text
      final AtomicReference<File> selectedFile = new AtomicReference<>(new File(clientSetting.value()));
      chooseFileButton.setOnAction(e -> {
        final DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setInitialDirectory(new File(clientSetting.value()));
        final File file = fileChooser.showDialog(chooseFileButton.getScene().getWindow());
        selectedFile.set(file);
        textField.setText(file.toString());
      });
      wrapper.getChildren().addAll(textField, chooseFileButton);
      return new SelectionComponent<Node>() {

        @Override
        public Node getJComponent() {
          return wrapper;
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
        public Map<GameSetting, String> readValues() {
          return Collections.singletonMap(clientSetting, selectedFile.get().toString());
        }

        @Override
        public void indicateError() {}

        @Override
        public void clearError() {}

        @Override
        public void resetToDefault() {
          textField.setText(clientSetting.defaultValue);
          selectedFile.set(new File(clientSetting.defaultValue));
        }

        @Override
        public void reset() {
          textField.setText(clientSetting.value());
          selectedFile.set(new File(clientSetting.value()));
        }
      };
    };
  }

  static Supplier<SelectionComponent<Node>> filePath(final ClientSetting clientSetting) {
    return () -> {
      final HBox wrapper = new HBox();
      final TextField textField = new TextField(clientSetting.value());
      textField.setDisable(true);
      final Button chooseFileButton = new Button("...");// Localize text
      final AtomicReference<File> selectedFile = new AtomicReference<>(new File(clientSetting.value()));
      chooseFileButton.setOnAction(e -> {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(selectedFile.get());
        final File file = fileChooser.showOpenDialog(chooseFileButton.getScene().getWindow());
        selectedFile.set(file);
        textField.setText(file.toString());
      });
      wrapper.getChildren().addAll(textField, chooseFileButton);
      return new SelectionComponent<Node>() {

        @Override
        public Node getJComponent() {
          return wrapper;
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
        public Map<GameSetting, String> readValues() {
          return Collections.singletonMap(clientSetting, selectedFile.get().toString());
        }

        @Override
        public void indicateError() {}

        @Override
        public void clearError() {}

        @Override
        public void resetToDefault() {
          textField.setText(clientSetting.defaultValue);
          selectedFile.set(new File(clientSetting.defaultValue));
        }

        @Override
        public void reset() {
          textField.setText(clientSetting.value());
          selectedFile.set(new File(clientSetting.value()));
        }
      };
    };
  }


  static Supplier<SelectionComponent<Node>> proxySettings() {
    return () -> {
      final Preferences pref = Preferences.userNodeForPackage(GameRunner.class);
      final HttpProxy.ProxyChoice proxyChoice =
          HttpProxy.ProxyChoice.valueOf(pref.get(HttpProxy.PROXY_CHOICE, HttpProxy.ProxyChoice.NONE.toString()));
      final RadioButton noneButton = new RadioButton("None");
      noneButton.setSelected(proxyChoice == HttpProxy.ProxyChoice.NONE);
      final RadioButton systemButton = new RadioButton("Use System Settings");
      systemButton.setSelected(proxyChoice == HttpProxy.ProxyChoice.USE_SYSTEM_SETTINGS);

      final RadioButton userButton = new RadioButton("Use These Settings:");
      userButton.setSelected(proxyChoice == HttpProxy.ProxyChoice.USE_USER_PREFERENCES);
      final TextField hostText = new TextField(ClientSetting.PROXY_HOST.value());
      final TextField portText = new TextField(ClientSetting.PROXY_PORT.value());
      final VBox radioPanel = new VBox();
      radioPanel.getChildren().addAll(
          noneButton,
          systemButton,
          userButton,
          new Label("Proxy Host: "),
          hostText,
          new Label("Proxy Port: "),
          portText);

      final ToggleGroup toggleGroup = new ToggleGroup();
      noneButton.setToggleGroup(toggleGroup);
      systemButton.setToggleGroup(toggleGroup);
      userButton.setToggleGroup(toggleGroup);

      return new SelectionComponent<Node>() {
        final EventHandler<ActionEvent> enableUserSettings = e -> {
          if (userButton.isSelected()) {
            hostText.setDisable(false);
            hostText.setStyle("-fx-background-color: #FFFFFF;");
            portText.setDisable(false);
            portText.setStyle("-fx-background-color: #FFFFFF;");
          } else {
            hostText.setDisable(true);
            hostText.setStyle("-fx-background-color: #404040;");
            portText.setDisable(true);
            portText.setStyle("-fx-background-color: #404040;");
          }
        };

        @Override
        public Node getJComponent() {
          enableUserSettings.handle(null);
          userButton.setOnAction(enableUserSettings);
          noneButton.setOnAction(enableUserSettings);
          systemButton.setOnAction(enableUserSettings);

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
            hostText.setStyle("-fx-background-color: #FF0000;");
          }
          if (!isPortTextValid()) {
            portText.setStyle("-fx-background-color: #FF0000;");
          }
        }

        @Override
        public void clearError() {
          hostText.setStyle("-fx-background-color: #FFFFFF;");
          portText.setStyle("-fx-background-color: #FFFFFF;");
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
    };
  }

  private static String getLocalizationKey(final Node rootNode, final ClientSetting clientSetting) {
    return "settings." + rootNode.getClass().getSimpleName().toLowerCase() + "." + clientSetting.name().toLowerCase();
  }
}
