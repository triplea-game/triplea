package org.triplea.game.client.ui.javafx.screens;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.framework.ui.GameChooserEntry;

import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.web.WebView;
import lombok.AccessLevel;
import lombok.Setter;

import org.triplea.game.client.parser.DefaultGameDetector;
import org.triplea.game.client.parser.GameDetector;
import org.triplea.game.client.ui.javafx.screen.ControlledScreen;
import org.triplea.game.client.ui.javafx.screen.ScreenController;
import org.triplea.game.client.ui.javafx.util.FxmlManager;
import org.triplea.util.LocalizeHtml;

public class MapSelection implements ControlledScreen<ScreenController<FxmlManager>> {

  private final GameDetector gameDetector;
  private final BiFunction<String, String, String> linkLocalizer;

  @Setter(value = AccessLevel.PACKAGE, onMethod_={@VisibleForTesting})
  @FXML private Node root;
  @FXML private TilePane mapContainer;
  @Setter(value = AccessLevel.PACKAGE, onMethod_={@VisibleForTesting})
  @FXML private Node previewContainer;
  @Setter(value = AccessLevel.PACKAGE, onMethod_={@VisibleForTesting})
  @FXML private WebView previewWindow;
  @FXML private Button selectButton;
  @FXML private Button detailsButton;

  @Setter(value = AccessLevel.PACKAGE, onMethod_={@VisibleForTesting})
  private GameChooserEntry selectedGame;
  private boolean loaded = false;

  private ScreenController<FxmlManager> screenController;

  // Constructor used via FXML reflection
  // to initialize this controller.
  @SuppressWarnings("unused")
  public MapSelection() {
    this(new DefaultGameDetector(), LocalizeHtml::localizeImgLinksInHtml);
  }

  @VisibleForTesting
  MapSelection(final GameDetector gameDetector, final BiFunction<String, String, String> linkLocalizer) {
    this.gameDetector = gameDetector;
    this.linkLocalizer = linkLocalizer;
  }

  private Node createMapListing(final GameChooserEntry gameChooserEntry) {
    final var button = new Button(gameChooserEntry.getGameName());
    // Placeholder image
    final var imageView = new ImageView("https://triplea-game.org/images/missing_map.png");
    imageView.setPreserveRatio(true);
    imageView.setFitHeight(150);
    imageView.setFitWidth(170);
    button.setGraphic(imageView);
    button.setWrapText(true);
    button.setPrefSize(200, 200);
    button.setContentDisplay(ContentDisplay.TOP);
    button.setOnAction(
        event -> {
          selectedGame = gameChooserEntry;
          selectButton.setDisable(false);
          detailsButton.setDisable(false);
          mapContainer.getChildren().forEach(node -> node.getStyleClass().remove("selected"));
          button.getStyleClass().add("selected");
        });
    return button;
  }

  @Override
  public void connect(ScreenController<FxmlManager> screenController) {
    this.screenController = screenController;
  }

  @Override
  public void onShow() {
    if (loaded) {
      return;
    }
    loaded = true;
    gameDetector.discoverGames(
        gameChooserEntries ->
            Platform.runLater(
                () -> {
                  mapContainer.setAlignment(Pos.TOP_LEFT);
                  mapContainer
                      .getChildren()
                      .setAll(
                          gameChooserEntries.stream()
                              .sorted()
                              .map(this::createMapListing)
                              .collect(Collectors.toList()));
                }));
  }

  @Override
  public Node getNode() {
    return root;
  }

  @FXML
  void showDetails() {
    previewContainer.setVisible(true);
    final String mapNameDir = selectedGame.getGameData().getProperties().get("mapName", "");
    final String trimmedNotes = selectedGame.getGameData().getProperties().get("notes", "").trim();
    final String notes = linkLocalizer.apply(trimmedNotes, mapNameDir);
    previewWindow.getEngine().loadContent(notes);
  }

  @FXML
  void closeDetails() {
    previewContainer.setVisible(false);
  }

  @FXML
  void backToGameSelection() {
    screenController.switchScreen(FxmlManager.GAME_SELECTION_CONTROLS);
  }
}
