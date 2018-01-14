package org.triplea.client.ui.javafx;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.UrlConstants;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.css.Styleable;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

class MainMenuPane extends BorderPane {

  private final TripleA triplea;
  private final DownloadPane downloadPane;
  private final SettingsPane settingsPane;

  @FXML
  private Text loggedInText;

  @FXML
  private HBox loginForm;

  @FXML
  private TextField username;

  @FXML
  private PasswordField password;

  @FXML
  private Button buttonBack;

  @FXML
  private Text version;

  @FXML
  private VBox aboutSection;

  @FXML
  private HBox gameOptions;

  @FXML
  private VBox mainOptions;

  /**
   * @param triplea The root pane.
   * @throws IOException If the FXML file is not present.
   */
  public MainMenuPane(final TripleA triplea) throws IOException {
    this.triplea = triplea;
    final FXMLLoader loader = FxmlManager.getLoader(getClass().getResource(FxmlManager.MAIN_MENU_PANE.toString()));
    loader.setRoot(this);
    loader.setController(this);
    loader.load();
    version.setText(MessageFormat.format(version.getText(), ClientContext.engineVersion().getExactVersion()));
    downloadPane = triplea.addRootContent(new DownloadPane(triplea));
    downloadPane.setVisible(false);
    settingsPane = triplea.addRootContent(new SettingsPane(triplea));
    settingsPane.setVisible(false);
    applyFileSelectionAnimation();
  }

  private void applyFileSelectionAnimation() {
    findChildrenWithClassRecursively(mainOptions, "button").stream().forEach(node -> {
      final Function<Node, NumberBinding> hoverBinding = n -> Bindings.when(n.hoverProperty()).then(-10).otherwise(0);
      final NumberBinding numberBinding = hoverBinding.apply(node);
      node.translateYProperty().bind(numberBinding.multiply(-1));
      node.getParent().translateYProperty().bind(!"mainOptions".equals(node.getParent().getParent().getId())
          ? Bindings.add(numberBinding,
              hoverBinding.apply(node.getParent().getParent().getChildrenUnmodifiable().get(0)).multiply(-1))
          : numberBinding);
    });
  }

  @FXML
  private void login() {}

  @FXML
  private void showLastMenu() {
    // TODO check which menu we are in
    aboutSection.setVisible(false);
    gameOptions.setVisible(false);
    buttonBack.setVisible(false);
    mainOptions.setVisible(true);
  }

  @FXML
  private void showHelp() {
    triplea.open(UrlConstants.GITHUB_HELP.toString());
  }

  @FXML
  private void showRuleBook() {
    triplea.open(UrlConstants.RULE_BOOK.toString());
  }

  @FXML
  private void showLobbyMenu() {}

  @FXML
  private void showLocalGameMenu() {}

  @FXML
  private void showHostNetworkGameMenu() {}

  @FXML
  private void showJoinNetworkGameMenu() {}

  @FXML
  private void showPbfPbemMenu() {}

  @FXML
  private void showPlayOptions() {
    mainOptions.setVisible(false);
    gameOptions.setVisible(true);
    buttonBack.setVisible(true);
  }

  @FXML
  private void showDownloadMenu() {
    setVisible(false);
    downloadPane.setVisible(true);
  }

  @FXML
  private void showSettingsMenu() {
    setVisible(false);
    settingsPane.setVisible(true);
  }

  @FXML
  private void showAboutSection() {
    mainOptions.setVisible(false);
    aboutSection.setVisible(true);
    buttonBack.setVisible(true);
  }

  @FXML
  private void showExitConfirmDialog() {
    triplea.promptExit();
  }

  @FXML
  private void startHover(final MouseEvent e) {}

  @FXML
  private void endHover(final MouseEvent e) {}


  private Set<Node> findChildrenWithClassRecursively(final Node node, final String cssClass) {
    final Set<Node> cssClasses = new HashSet<>();
    if (node instanceof Parent) {
      for (final Node child : ((Parent) node).getChildrenUnmodifiable()) {
        cssClasses.addAll(findChildrenWithClassRecursively(child, cssClass));
      }
    }
    if (((Styleable) node).getStyleClass().contains(cssClass)) {
      cssClasses.add(node);
    }
    return cssClasses;
  }
}
