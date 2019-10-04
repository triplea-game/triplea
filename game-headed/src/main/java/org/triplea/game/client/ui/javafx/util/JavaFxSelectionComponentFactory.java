package org.triplea.game.client.ui.javafx.util;

import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.settings.SelectionComponent;
import games.strategy.triplea.settings.SelectionComponentUiUtils;
import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiFunction;
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
import javafx.stage.Window;
import javax.annotation.Nullable;
import org.triplea.java.OptionalUtils;

final class JavaFxSelectionComponentFactory {

  private JavaFxSelectionComponentFactory() {}

  static SelectionComponent<Region> intValueRange(
      final ClientSetting<Integer> clientSetting, final int minValue, final int maxValue) {
    return intValueRange(clientSetting, minValue, maxValue, false);
  }

  static SelectionComponent<Region> intValueRange(
      final ClientSetting<Integer> clientSetting,
      final int minValue,
      final int maxValue,
      final boolean allowUnset) {
    return new SelectionComponent<>() {
      private final Spinner<Integer> spinner = newSpinner();

      private Spinner<Integer> newSpinner() {
        final Spinner<Integer> spinner =
            new Spinner<>(
                minValue - (allowUnset ? 1 : 0),
                maxValue,
                getIntegerFromOptional(clientSetting.getValue()));
        spinner.setEditable(true);
        spinner
            .focusedProperty()
            .addListener(
                (observable, oldValue, newValue) -> {
                  if (!newValue) {
                    spinner.increment(0); // hack to force editor to commit value when losing focus
                  }
                });
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
      public void save(final SaveContext context) {
        final Integer spinnerValue = spinner.getValue();
        final @Nullable Integer value =
            (allowUnset && spinnerValue.equals(unsetValue())) ? null : spinnerValue;
        context.setValue(clientSetting, value);
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
    return new SelectionComponent<>() {
      final CheckBox checkBox = getCheckBox();

      private CheckBox getCheckBox() {
        final CheckBox checkBox = new CheckBox();
        checkBox.setSelected(clientSetting.getValueOrThrow());
        return checkBox;
      }

      @Override
      public Region getUiComponent() {
        return checkBox;
      }

      @Override
      public void save(final SaveContext context) {
        context.setValue(clientSetting, checkBox.selectedProperty().get());
      }

      @Override
      public void resetToDefault() {
        checkBox.selectedProperty().set(clientSetting.getDefaultValue().orElse(false));
      }

      @Override
      public void reset() {
        checkBox.selectedProperty().set(clientSetting.getValueOrThrow());
      }
    };
  }

  static SelectionComponent<Region> textField(final ClientSetting<String> clientSetting) {
    return new SelectionComponent<>() {
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
      public void save(final SaveContext context) {
        final String value = textField.getText();
        context.setValue(clientSetting, value.isEmpty() ? null : value);
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

  static SelectionComponent<Region> folderPath(final ClientSetting<Path> clientSetting) {
    return new FileSelector(
        clientSetting,
        (window, selectedPath) -> {
          final DirectoryChooser fileChooser = new DirectoryChooser();
          if (selectedPath != null) {
            fileChooser.setInitialDirectory(selectedPath.toFile());
          }
          return Optional.ofNullable(fileChooser.showDialog(window)).map(File::toPath).orElse(null);
        });
  }

  static SelectionComponent<Region> filePath(final ClientSetting<Path> clientSetting) {
    return new FileSelector(
        clientSetting,
        (window, selectedPath) -> {
          final FileChooser fileChooser = new FileChooser();
          if (selectedPath != null) {
            fileChooser.setInitialDirectory(selectedPath.getParent().toFile());
          }
          return Optional.ofNullable(fileChooser.showOpenDialog(window))
              .map(File::toPath)
              .orElse(null);
        });
  }

  static SelectionComponent<Region> proxySettings(
      final ClientSetting<HttpProxy.ProxyChoice> proxyChoiceClientSetting,
      final ClientSetting<String> proxyHostClientSetting,
      final ClientSetting<Integer> proxyPortClientSetting) {
    return new ProxySetting(
        proxyChoiceClientSetting, proxyHostClientSetting, proxyPortClientSetting);
  }

  private static final class FileSelector extends Region implements SelectionComponent<Region> {
    private final ClientSetting<Path> clientSetting;
    private final TextField textField;
    private @Nullable Path selectedPath;

    FileSelector(
        final ClientSetting<Path> clientSetting,
        final BiFunction<Window, /* @Nullable */ Path, /* @Nullable */ Path> chooseFile) {
      this.clientSetting = clientSetting;
      final @Nullable Path initialValue = clientSetting.getValue().orElse(null);
      final HBox wrapper = new HBox();
      textField = new TextField(SelectionComponentUiUtils.toString(clientSetting.getValue()));
      textField
          .prefColumnCountProperty()
          .bind(Bindings.add(1, Bindings.length(textField.textProperty())));
      textField.setMaxWidth(Double.MAX_VALUE);
      textField.setMinWidth(100);
      textField.setDisable(true);
      final Button chooseFileButton = new Button("...");
      selectedPath = initialValue;
      chooseFileButton.setOnAction(
          e -> {
            final @Nullable Path path =
                chooseFile.apply(chooseFileButton.getScene().getWindow(), selectedPath);
            if (path != null) {
              selectedPath = path;
              textField.setText(path.toString());
            }
          });
      wrapper.getChildren().addAll(textField, chooseFileButton);
      getChildren().add(wrapper);
    }

    @Override
    public void save(final SaveContext context) {
      context.setValue(clientSetting, selectedPath);
    }

    @Override
    public void resetToDefault() {
      reset(clientSetting.getDefaultValue());
    }

    @Override
    public void reset() {
      reset(clientSetting.getValue());
    }

    private void reset(final Optional<Path> path) {
      textField.setText(SelectionComponentUiUtils.toString(path));
      selectedPath = path.orElse(null);
    }

    @Override
    public Region getUiComponent() {
      return this;
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

      final HttpProxy.ProxyChoice proxyChoice = proxyChoiceClientSetting.getValueOrThrow();
      noneButton = new RadioButton("None");
      noneButton.setSelected(proxyChoice == HttpProxy.ProxyChoice.NONE);
      systemButton = new RadioButton("Use System Settings");
      systemButton.setSelected(proxyChoice == HttpProxy.ProxyChoice.USE_SYSTEM_SETTINGS);
      userButton = new RadioButton("Use These Settings:");
      userButton.setSelected(proxyChoice == HttpProxy.ProxyChoice.USE_USER_PREFERENCES);
      hostText = new TextField(proxyHostClientSetting.getValue().orElse(""));
      portText = new TextField(proxyPortClientSetting.getValue().map(Object::toString).orElse(""));
      final VBox radioPanel = new VBox();
      radioPanel
          .getChildren()
          .addAll(
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
      toggleGroup
          .selectedToggleProperty()
          .addListener(
              (observable, oldValue, newValue) -> {
                if (!userButton.isSelected()) {
                  hostText.clear();
                  portText.clear();
                }
              });

      getChildren().add(radioPanel);
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
        OptionalUtils.ifEmpty(
            optionalHost,
            () ->
                context.reportError(
                    proxyHostClientSetting,
                    "must be a network name or an IP address",
                    encodedHost));

        final String encodedPort = portText.getText().trim();
        final Optional<Integer> optionalPort = parsePort(encodedPort);
        OptionalUtils.ifEmpty(
            optionalPort,
            () ->
                context.reportError(
                    proxyPortClientSetting,
                    "must be a positive integer, usually 4 to 5 digits",
                    encodedPort));

        OptionalUtils.ifAllPresent(
            optionalHost,
            optionalPort,
            (host, port) -> {
              context.setValue(
                  proxyChoiceClientSetting, HttpProxy.ProxyChoice.USE_USER_PREFERENCES);
              context.setValue(proxyHostClientSetting, host);
              context.setValue(proxyPortClientSetting, port);
            });
      }
    }

    private static Optional<String> parseHost(final String encodedHost) {
      return !encodedHost.isEmpty() ? Optional.of(encodedHost) : Optional.empty();
    }

    private static Optional<Integer> parsePort(final String encodedPort) {
      try {
        final Integer port = Integer.valueOf(encodedPort);
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
      setProxyChoice(proxyChoiceClientSetting.getValueOrThrow());
    }

    @Override
    public Region getUiComponent() {
      return this;
    }
  }
}
