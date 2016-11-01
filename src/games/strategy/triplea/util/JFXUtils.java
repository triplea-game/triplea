package games.strategy.triplea.util;

import java.awt.image.BufferedImage;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;

public final class JFXUtils {

  private JFXUtils() {}

  public static MenuItem getMenuButton(String text, EventHandler<ActionEvent> e) {
    MenuItem item = new MenuItem(text);
    item.setMnemonicParsing(true);
    item.setOnAction(e);
    return item;
  }

  public static Alert getDialog(AlertType type, String title, String headerText, String contentText,
      ButtonType... types) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(headerText);
    alert.setContentText(contentText);
    alert.getButtonTypes().addAll(types);
    return alert;
  }

  public static Button getButtonWithAction(EventHandler<ActionEvent> value) {
    Button button = new Button();
    button.setOnAction(value);
    return button;
  }

  public static void showInfoDialog(String title, String headerText, String contentText) {
    getDialog(AlertType.INFORMATION, contentText, contentText, contentText).showAndWait();
  }

  public static void showWarningDialog(String title, String headerText, String contentText) {
    getDialog(AlertType.WARNING, contentText, contentText, contentText).showAndWait();
  }

  public static void showErrorDialog(String title, String headerText, String contentText) {
    getDialog(AlertType.ERROR, contentText, contentText, contentText).showAndWait();
  }

  public static Alert getDialogWithContent(Node content, AlertType type, String title, String headerText,
      String contentText, ButtonType... types) {
    Alert alert = getDialog(type, contentText, contentText, contentText, types);
    alert.getDialogPane().setContent(content);
    return alert;
  }

  public static Image convertToFx(BufferedImage oldImg) {
    return SwingFXUtils.toFXImage(oldImg, null);
  }
}
