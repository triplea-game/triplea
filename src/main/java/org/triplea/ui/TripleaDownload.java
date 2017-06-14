package org.triplea.ui;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

public class TripleaDownload extends VBox {

  private TripleA triplea;

  public TripleaDownload(TripleA triplea) throws IOException {
    FXMLLoader loader = TripleA.getLoader(getClass().getResource("./fxml/TripleADownload.fxml"));
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
