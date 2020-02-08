package org.triplea.game.client.ui.javafx.controls;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.SkinBase;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/**
 * A skin that renders a check box as a box with an image for the selected state.
 *
 * <p>The following CSS classes are used to control the rendering:
 *
 * <ul>
 *   <li>{@code .box}: The base check box.
 *   <li>{@code .mark}: The image displayed on top of the box when the check box is selected.
 * </ul>
 */
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
    StackPane.setAlignment(view, Pos.BOTTOM_LEFT);
    view.visibleProperty().bind(getSkinnable().selectedProperty());
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
    pane.setOnMouseClicked(e -> getSkinnable().fire());
    getChildren().add(pane);
  }

  private void layoutRect() {
    StackPane.setAlignment(rect, Pos.BOTTOM_LEFT);
    rect.getStyleClass().setAll("box");
    rect.widthProperty()
        .bind(
            Bindings.multiply(Bindings.min(pane.widthProperty(), pane.heightProperty()), 2.0 / 3));
    rect.heightProperty()
        .bind(
            Bindings.multiply(Bindings.min(pane.widthProperty(), pane.heightProperty()), 2.0 / 3));
    pane.getChildren().add(rect);
  }
}
