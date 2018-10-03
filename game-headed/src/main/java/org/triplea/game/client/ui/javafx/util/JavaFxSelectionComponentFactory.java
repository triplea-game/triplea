package org.triplea.game.client.ui.javafx.util;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.prefs.Preferences;

import com.google.common.base.Strings;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.settings.GameSetting;
import games.strategy.triplea.settings.SelectionComponent;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

final class JavaFxSelectionComponentFactory {

  private JavaFxSelectionComponentFactory() {}

  static SelectionComponent<Region> intValueRange(
      final ClientSetting clientSetting,
      final int minValue,
      final int maxValue) {
    return intValueRange(clientSetting, minValue, maxValue, false);
  }

  static SelectionComponent<Region> intValueRange(
      final ClientSetting clientSetting,
      final int minValue,
      final int maxValue,
      final boolean allowUnset) {
    return new SelectionComponent<Region>() {

      final Spinner<Integer> spinner = createSpinner();

      private Spinner<Integer> createSpinner() {
        final Spinner<Integer> spinner = new Spinner<>(
            minValue - (allowUnset ? 1 : 0),
            maxValue,
            getIntegerFromString(clientSetting.value()));
        spinner.setEditable(true);
        return spinner;
      }

      @Override
      public Region getUiComponent() {
        return spinner;
      }

      private Integer getIntegerFromString(final String string) {
        return string.isEmpty() && allowUnset
            ? minValue - 1
            : Integer.valueOf(string);
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
        final Integer value = spinner.getValue();
        final String stringValue = allowUnset && value == minValue - 1 ? "" : value.toString();
        return Collections.singletonMap(clientSetting, stringValue);
      }

      @Override
      public void resetToDefault() {
        spinner.getValueFactory().setValue(getIntegerFromString(clientSetting.defaultValue));
      }

      @Override
      public void reset() {
        spinner.getValueFactory().setValue(getIntegerFromString(clientSetting.value()));
      }
    };
  }

  static SelectionComponent<Region> toggleButton(final ClientSetting clientSetting) {
    return new SelectionComponent<Region>() {
      final CheckBox checkBox = getCheckBox();

      private CheckBox getCheckBox() {
        final CheckBox checkBox = new CheckBox();
        checkBox.setSelected(Boolean.parseBoolean(clientSetting.value()));
        return checkBox;
      }

      @Override
      public Region getUiComponent() {
        return checkBox;
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
        return Collections.singletonMap(clientSetting, String.valueOf(checkBox.selectedProperty().get()));
      }

      @Override
      public void resetToDefault() {
        checkBox.selectedProperty().set(Boolean.parseBoolean(clientSetting.defaultValue));
      }

      @Override
      public void reset() {
        checkBox.selectedProperty().set(Boolean.parseBoolean(clientSetting.value()));
      }
    };
  }

  static SelectionComponent<Region> textField(final ClientSetting clientSetting) {
    return new SelectionComponent<Region>() {
      final TextField textField = newTextField();

      private TextField newTextField() {
        final TextField textField = new TextField();
        textField.setText(clientSetting.value());
        return textField;
      }

      @Override
      public Region getUiComponent() {
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
      public void resetToDefault() {
        textField.setText(clientSetting.defaultValue);
      }

      @Override
      public void reset() {
        textField.setText(clientSetting.value());
      }
    };
  }

  static SelectionComponent<Region> folderPath(final ClientSetting clientSetting) {
    return new FolderSelector(clientSetting);
  }

  static SelectionComponent<Region> filePath(final ClientSetting clientSetting) {
    return new FileSelector(clientSetting);
  }

  static SelectionComponent<Region> proxySettings(
      final ClientSetting proxyChoiceClientSetting,
      final ClientSetting proxyHostClientSetting,
      final ClientSetting proxyPortClientSetting) {
    return new ProxySetting(proxyChoiceClientSetting, proxyHostClientSetting, proxyPortClientSetting);
  }

  private static final class FolderSelector extends Region implements SelectionComponent<Region> {
    private final ClientSetting clientSetting;
    private final TextField textField;
    private File selectedFile;

    FolderSelector(final ClientSetting clientSetting) {
      this.clientSetting = clientSetting;
      final File initialValue = clientSetting.value().isEmpty() ? null : new File(clientSetting.value());
      final HBox wrapper = new HBox();
      textField = new TextField(clientSetting.value());
      textField.prefColumnCountProperty().bind(Bindings.add(1, Bindings.length(textField.textProperty())));
      textField.setMaxWidth(Double.MAX_VALUE);
      textField.setDisable(true);
      final Button chooseFileButton = new Button("...");
      selectedFile = initialValue;
      chooseFileButton.setOnAction(e -> {
        final DirectoryChooser fileChooser = new DirectoryChooser();
        if (selectedFile != null) {
          fileChooser.setInitialDirectory(selectedFile);
        }
        final File file = fileChooser.showDialog(chooseFileButton.getScene().getWindow());
        if (file != null) {
          selectedFile = file;
          textField.setText(file.toString());
        }
      });
      wrapper.getChildren().addAll(textField, chooseFileButton);
      getChildren().add(wrapper);
    }

    @Override
    public Map<GameSetting, String> readValues() {
      return Collections.singletonMap(clientSetting, Objects.toString(selectedFile, ""));
    }

    @Override
    public void resetToDefault() {
      textField.setText(clientSetting.defaultValue);
      selectedFile = new File(clientSetting.defaultValue);
    }

    @Override
    public void reset() {
      textField.setText(clientSetting.value());
      selectedFile = new File(clientSetting.value());
    }

    @Override
    public Region getUiComponent() {
      return this;
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

  private static final class FileSelector extends Region implements SelectionComponent<Region> {
    private final ClientSetting clientSetting;
    private final TextField textField;
    private File selectedFile;

    FileSelector(final ClientSetting clientSetting) {
      this.clientSetting = clientSetting;
      final File initialValue = clientSetting.value().isEmpty() ? null : new File(clientSetting.value());
      final HBox wrapper = new HBox();
      textField = new TextField(clientSetting.value());
      textField.prefColumnCountProperty().bind(Bindings.add(1, Bindings.length(textField.textProperty())));
      textField.setMaxWidth(Double.MAX_VALUE);
      textField.setMinWidth(100);
      textField.setDisable(true);
      final Button chooseFileButton = new Button("...");
      selectedFile = initialValue;
      chooseFileButton.setOnAction(e -> {
        final FileChooser fileChooser = new FileChooser();
        if (selectedFile != null) {
          fileChooser.setInitialDirectory(selectedFile);
        }
        final File file = fileChooser.showOpenDialog(chooseFileButton.getScene().getWindow());
        if (file != null) {
          selectedFile = file;
          textField.setText(file.toString());
        }
      });
      wrapper.getChildren().addAll(textField, chooseFileButton);
      getChildren().add(wrapper);
    }

    @Override
    public Map<GameSetting, String> readValues() {
      return Collections.singletonMap(clientSetting, Objects.toString(selectedFile, ""));
    }

    @Override
    public void resetToDefault() {
      textField.setText(clientSetting.defaultValue);
      selectedFile = new File(clientSetting.defaultValue);
    }

    @Override
    public void reset() {
      textField.setText(clientSetting.value());
      selectedFile = new File(clientSetting.value());
    }

    @Override
    public Region getUiComponent() {
      return this;
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

  private static final class ProxySetting extends Region implements SelectionComponent<Region> {
    private final ClientSetting proxyChoiceClientSetting;
    private final ClientSetting proxyHostClientSetting;
    private final ClientSetting proxyPortClientSetting;
    private final RadioButton noneButton;
    private final RadioButton systemButton;
    private final RadioButton userButton;
    private final TextField hostText;
    private final TextField portText;

    ProxySetting(
        final ClientSetting proxyChoiceClientSetting,
        final ClientSetting proxyHostClientSetting,
        final ClientSetting proxyPortClientSetting) {
      this.proxyChoiceClientSetting = proxyChoiceClientSetting;
      this.proxyHostClientSetting = proxyHostClientSetting;
      this.proxyPortClientSetting = proxyPortClientSetting;
      final Preferences pref = Preferences.userNodeForPackage(GameRunner.class);
      final HttpProxy.ProxyChoice proxyChoice =
          HttpProxy.ProxyChoice.valueOf(pref.get(HttpProxy.PROXY_CHOICE, HttpProxy.ProxyChoice.NONE.toString()));
      noneButton = new RadioButton("None");
      noneButton.setSelected(proxyChoice == HttpProxy.ProxyChoice.NONE);
      systemButton = new RadioButton("Use System Settings");
      systemButton.setSelected(proxyChoice == HttpProxy.ProxyChoice.USE_SYSTEM_SETTINGS);

      userButton = new RadioButton("Use These Settings:");
      userButton.setSelected(proxyChoice == HttpProxy.ProxyChoice.USE_USER_PREFERENCES);
      hostText = new TextField(proxyHostClientSetting.value());
      portText = new TextField(proxyPortClientSetting.value());
      final VBox radioPanel = new VBox();
      radioPanel.getChildren().addAll(
          noneButton,
          systemButton,
          userButton,
          new Label("Proxy Host: "),
          hostText,
          new Label("Proxy Port: "),
          portText);
      hostText.disableProperty().bind(Bindings.not(userButton.selectedProperty()));
      portText.disableProperty().bind(Bindings.not(userButton.selectedProperty()));

      final ToggleGroup toggleGroup = new ToggleGroup();
      noneButton.setToggleGroup(toggleGroup);
      systemButton.setToggleGroup(toggleGroup);
      userButton.setToggleGroup(toggleGroup);
      getChildren().add(radioPanel);
    }

    @Override
    public Map<GameSetting, String> readValues() {
      final Map<GameSetting, String> values = new HashMap<>();
      if (noneButton.isSelected()) {
        values.put(proxyChoiceClientSetting, HttpProxy.ProxyChoice.NONE.toString());
      } else if (systemButton.isSelected()) {
        values.put(proxyChoiceClientSetting, HttpProxy.ProxyChoice.USE_SYSTEM_SETTINGS.toString());
        HttpProxy.updateSystemProxy();
      } else {
        values.put(proxyChoiceClientSetting, HttpProxy.ProxyChoice.USE_USER_PREFERENCES.toString());
        values.put(proxyHostClientSetting, hostText.getText().trim());
        values.put(proxyPortClientSetting, portText.getText().trim());
      }
      return values;
    }

    @Override
    public void resetToDefault() {
      ClientSetting.flush();
      hostText.setText(proxyHostClientSetting.defaultValue);
      portText.setText(proxyPortClientSetting.defaultValue);
      noneButton.setSelected(Boolean.valueOf(proxyChoiceClientSetting.defaultValue));
    }

    @Override
    public void reset() {
      ClientSetting.flush();
      hostText.setText(proxyHostClientSetting.value());
      portText.setText(proxyPortClientSetting.value());
      noneButton.setSelected(proxyChoiceClientSetting.booleanValue());
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
    public boolean isValid() {
      return !userButton.isSelected() || (isHostTextValid() && isPortTextValid());
    }

    @Override
    public Region getUiComponent() {
      return this;
    }

    @Override
    public String validValueDescription() {
      return "Proxy host can be a network name or an IP address, port should be number, usually 4 to 5 digits.";
    }
  }
}
