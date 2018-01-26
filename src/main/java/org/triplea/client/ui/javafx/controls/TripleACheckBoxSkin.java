package org.triplea.client.ui.javafx.controls;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.SkinBase;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class TripleACheckBoxSkin extends SkinBase<CheckBox> {

  private final StackPane pane = new StackPane();
  private final ImageView view = new ImageView();
  private final Rectangle rect = new Rectangle();


  public TripleACheckBoxSkin(final CheckBox control) {
    super(control);
    layoutPane();
    layoutRect();
    layoutImage();
  }

  private void layoutImage() {
    view.visibleProperty().bind(getSkinnable().selectedProperty());
    view.setImage(new Image(getClass().getResourceAsStream("/org/triplea/client/ui/javafx/images/checkmark.png")));
    view.getStyleClass().setAll("mark");
    pane.getChildren().add(view);
  }

  private void layoutPane() {
    pane.minWidthProperty().bind(getSkinnable().minWidthProperty());
    pane.minHeightProperty().bind(getSkinnable().minHeightProperty());
    pane.prefWidthProperty().bind(getSkinnable().prefWidthProperty());
    pane.prefHeightProperty().bind(getSkinnable().prefHeightProperty());
    pane.maxWidthProperty().bind(getSkinnable().maxWidthProperty());
    pane.maxHeightProperty().bind(getSkinnable().maxHeightProperty());
    pane.setOnMouseClicked(e -> getSkinnable().selectedProperty().set(!getSkinnable().selectedProperty().get()));
    getChildren().add(pane);
  }

  private void layoutRect() {
    StackPane.setAlignment(rect, Pos.BOTTOM_LEFT);
    rect.getStyleClass().setAll("mark");
    rect.setStroke(Color.DARKOLIVEGREEN);
    rect.setStrokeWidth(5);
    rect.setFill(Color.ANTIQUEWHITE);
    rect.widthProperty().bind(Bindings.multiply(pane.widthProperty(), 2.0 / 3));
    rect.heightProperty().bind(Bindings.multiply(pane.heightProperty(), 2.0 / 3));
    pane.getChildren().add(rect);
  }
}
