package org.triplea.game.client.ui.javafx;

import java.io.IOException;

import org.triplea.game.client.ui.javafx.util.FxmlManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

class DownloadPane extends VBox {

  private final TripleA triplea;

  /**
   * Initializes a new instance of the DownloadPane class.
   *
   * @param triplea The root pane.
   * @throws IOException If the FXML file is not present.
   */
  DownloadPane(final TripleA triplea) throws IOException {
    final FXMLLoader loader = FxmlManager.getLoader(getClass().getResource(FxmlManager.DOWNLOAD_PANE.toString()));
    loader.setRoot(this);
    loader.setController(this);
    loader.load();
    this.triplea = triplea;
  }

  @FXML
  private void back() {
    triplea.returnToMainMenu(this);
  }
}
