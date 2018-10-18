package org.triplea.game.client.ui.javafx.util;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.common.base.Strings;

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
      final ClientSetting<Integer> clientSetting,
      final int minValue,
      final int maxValue) {
    return intValueRange(clientSetting, minValue, maxValue, false);
  }

  static SelectionComponent<Region> intValueRange(
      final ClientSetting<Integer> clientSetting,
      final int minValue,
      final int maxValue,
      final boolean allowUnset) {
    return new SelectionComponent<Region>() {
      final Spinner<Integer> spinner = createSpinner();

      private Spinner<Integer> createSpinner() {
        final Spinner<Integer> spinner = new Spinner<>(
            minValue - (allowUnset ? 1 : 0),
            maxValue,
            getIntegerFromOptional(clientSetting.getValue()));
        spinner.setEditable(true);
        return spinner;
      }

      @Override
      public Region getUiComponent() {
        return spinner;
      }

      private Integer getIntegerFromOptional(final Optional<Integer> value) {
        return value.orElseGet(this::unsetValue);
      }

      private Integer unsetValue() {
        return minValue - 1;
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
        final Integer spinnerValue = spinner.getValue();
        final @Nullable Integer value = (allowUnset && spinnerValue.equals(unsetValue())) ? null : spinnerValue;
        return Collections.singletonMap(clientSetting, value);
      }

      @Override
      public void resetToDefault() {
        spinner.getValueFactory().setValue(getIntegerFromOptional(clientSetting.getDefaultValue()));
      }

      @Override
      public void reset() {
        spinner.getValueFactory().setValue(getIntegerFromOptional(clientSetting.getValue()));
      }
    };
  }

  static SelectionComponent<Region> toggleButton(final ClientSetting<Boolean> clientSetting) {
    return new SelectionComponent<Region>() {
      final CheckBox checkBox = getCheckBox();

      private CheckBox getCheckBox() {
        final CheckBox checkBox = new CheckBox();
        checkBox.setSelected(clientSetting.value());
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
      public Map<GameSetting<?>, Object> readValues() {
        return Collections.singletonMap(clientSetting, checkBox.selectedProperty().get());
      }

      @Override
      public void resetToDefault() {
        checkBox.selectedProperty().set(clientSetting.getDefaultValue().orElse(false));
      }

      @Override
      public void reset() {
        checkBox.selectedProperty().set(clientSetting.value());
      }
    };
  }

  static SelectionComponent<Region> textField(final ClientSetting<String> clientSetting) {
    return new SelectionComponent<Region>() {
      final TextField textField = newTextField();

      private TextField newTextField() {
        final TextField textField = new TextField();
        textField.setText(clientSetting.getValue().orElse(""));
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
      public Map<GameSetting<?>, Object> readValues() {
        final String value = textField.getText();
        return Collections.singletonMap(clientSetting, value.isEmpty() ? null : value);
      }

      @Override
      public void resetToDefault() {
        textField.setText(clientSetting.getDefaultValue().orElse(""));
      }

      @Override
      public void reset() {
        textField.setText(clientSetting.getValue().orElse(""));
      }
    };
  }

  static SelectionComponent<Region> folderPath(final ClientSetting<String> clientSetting) {
    return new FolderSelector(clientSetting);
  }

  static SelectionComponent<Region> filePath(final ClientSetting<String> clientSetting) {
    return new FileSelector(clientSetting);
  }

  static SelectionComponent<Region> proxySettings(
      final ClientSetting<HttpProxy.ProxyChoice> proxyChoiceClientSetting,
      final ClientSetting<String> proxyHostClientSetting,
      final ClientSetting<Integer> proxyPortClientSetting) {
    return new ProxySetting(proxyChoiceClientSetting, proxyHostClientSetting, proxyPortClientSetting);
  }

  private static final class FolderSelector extends Region implements SelectionComponent<Region> {
    private final ClientSetting<String> clientSetting;
    private final TextField textField;
    private @Nullable File selectedFile;

    FolderSelector(final ClientSetting<String> clientSetting) {
      this.clientSetting = clientSetting;
      final @Nullable File initialValue = clientSetting.getValue().map(File::new).orElse(null);
      final HBox wrapper = new HBox();
      textField = new TextField(clientSetting.getValue().orElse(""));
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
        final @Nullable File file = fileChooser.showDialog(chooseFileButton.getScene().getWindow());
        if (file != null) {
          selectedFile = file;
          textField.setText(file.toString());
        }
      });
      wrapper.getChildren().addAll(textField, chooseFileButton);
      getChildren().add(wrapper);
    }

    @Override
    public Map<GameSetting<?>, Object> readValues() {
      return Collections.singletonMap(clientSetting, Objects.toString(selectedFile, null));
    }

    @Override
    public void resetToDefault() {
      textField.setText(clientSetting.getDefaultValue().orElse(""));
      selectedFile = clientSetting.getDefaultValue().map(File::new).orElse(null);
    }

    @Override
    public void reset() {
      textField.setText(clientSetting.getValue().orElse(""));
      selectedFile = clientSetting.getValue().map(File::new).orElse(null);
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
    private final ClientSetting<String> clientSetting;
    private final TextField textField;
    private @Nullable File selectedFile;

    FileSelector(final ClientSetting<String> clientSetting) {
      this.clientSetting = clientSetting;
      final @Nullable File initialValue = clientSetting.getValue().map(File::new).orElse(null);
      final HBox wrapper = new HBox();
      textField = new TextField(clientSetting.getValue().orElse(""));
      textField.prefColumnCountProperty().bind(Bindings.add(1, Bindings.length(textField.textProperty())));
      textField.setMaxWidth(Double.MAX_VALUE);
      textField.setMinWidth(100);
      textField.setDisable(true);
      final Button chooseFileButton = new Button("...");
      selectedFile = initialValue;
      chooseFileButton.setOnAction(e -> {
        final FileChooser fileChooser = new FileChooser();
        if (selectedFile != null) {
          fileChooser.setInitialDirectory(selectedFile.getParentFile());
        }
        final @Nullable File file = fileChooser.showOpenDialog(chooseFileButton.getScene().getWindow());
        if (file != null) {
          selectedFile = file;
          textField.setText(file.toString());
        }
      });
      wrapper.getChildren().addAll(textField, chooseFileButton);
      getChildren().add(wrapper);
    }

    @Override
    public Map<GameSetting<?>, Object> readValues() {
      return Collections.singletonMap(clientSetting, Objects.toString(selectedFile, null));
    }

    @Override
    public void resetToDefault() {
      textField.setText(clientSetting.getDefaultValue().orElse(""));
      selectedFile = clientSetting.getDefaultValue().map(File::new).orElse(null);
    }

    @Override
    public void reset() {
      textField.setText(clientSetting.getValue().orElse(""));
      selectedFile = clientSetting.getValue().map(File::new).orElse(null);
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
    private final ClientSetting<HttpProxy.ProxyChoice> proxyChoiceClientSetting;
    private final ClientSetting<String> proxyHostClientSetting;
    private final ClientSetting<Integer> proxyPortClientSetting;
    private final RadioButton noneButton;
    private final RadioButton systemButton;
    private final RadioButton userButton;
    private final TextField hostText;
    private final TextField portText;

    ProxySetting(
        final ClientSetting<HttpProxy.ProxyChoice> proxyChoiceClientSetting,
        final ClientSetting<String> proxyHostClientSetting,
        final ClientSetting<Integer> proxyPortClientSetting) {
      this.proxyChoiceClientSetting = proxyChoiceClientSetting;
      this.proxyHostClientSetting = proxyHostClientSetting;
      this.proxyPortClientSetting = proxyPortClientSetting;

      final HttpProxy.ProxyChoice proxyChoice = proxyChoiceClientSetting.value();
      noneButton = new RadioButton("None");
      noneButton.setSelected(proxyChoice == HttpProxy.ProxyChoice.NONE);
      systemButton = new RadioButton("Use System Settings");
      systemButton.setSelected(proxyChoice == HttpProxy.ProxyChoice.USE_SYSTEM_SETTINGS);
      userButton = new RadioButton("Use These Settings:");
      userButton.setSelected(proxyChoice == HttpProxy.ProxyChoice.USE_USER_PREFERENCES);
      hostText = new TextField(proxyHostClientSetting.getValue().orElse(""));
      portText = new TextField(proxyPortClientSetting.getValue().map(Object::toString).orElse(""));
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
    }

    @Override
    public void reset() {
      ClientSetting.flush();
      hostText.setText(proxyHostClientSetting.getValue().orElse(""));
      portText.setText(proxyPortClientSetting.getValue().map(Object::toString).orElse(""));
      setProxyChoice(proxyChoiceClientSetting.value());
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
