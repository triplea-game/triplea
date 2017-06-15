package org.triplea.ui;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

public class TripleaDownload extends VBox {

  private final TripleA triplea;

  /**
   * Public constructor.
   * @param triplea The root pane
   * @throws IOException If the FXML file is not present
   */
  public TripleaDownload(final TripleA triplea) throws IOException {
    final FXMLLoader loader = TripleA.getLoader(getClass().getResource("./fxml/TripleADownload.fxml"));
    loader.setRoot(this);
    loader.setController(this);
    loader.load();
    this.triplea = triplea;
  }

  @FXML
  private void back() {
    setVisible(false);
    triplea.getMainMenu().setVisible(true);
  }
}
