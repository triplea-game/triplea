package org.triplea.client.ui.javafx;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

public class SettingsPane extends StackPane {
  private final TripleA triplea;

  @FXML
  private ComboBox<String> proxyCombobox;
  @FXML
  private TextField proxyHost;
  @FXML
  private Spinner<Integer> proxyPort;

  /**
   * @param triplea The root pane.
   * @throws IOException If the FXML file is not present.
   */
  public SettingsPane(final TripleA triplea) throws IOException {
    final FXMLLoader loader = FxmlManager.getLoader(getClass().getResource(FxmlManager.SETTINGS_PANE.toString()));
    loader.setRoot(this);
    loader.setController(this);
    loader.load();
    this.triplea = triplea;
    final List<String> newItems =
        proxyCombobox.getItems().stream().map(s -> s.substring(1)).map(loader.getResources()::getString)
            .collect(Collectors.toList());
    proxyCombobox.getItems().clear();
    proxyCombobox.getItems().addAll(newItems);
    proxyCombobox.getSelectionModel().selectFirst();
    proxyCombobox.valueProperty().addListener((a, b, c) -> {
      final boolean disabled = proxyCombobox.getSelectionModel().getSelectedIndex() != 2;
      proxyHost.setDisable(disabled);
      proxyPort.setDisable(disabled);
    });
  }

  @FXML
  private void back() {
    triplea.returnToMainMenu(this);
  }

  @FXML
  private void reset() {}

  @FXML
  private void save() {}
}
