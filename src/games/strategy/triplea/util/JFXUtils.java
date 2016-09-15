package games.strategy.triplea.util;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Alert.AlertType;

public final class JFXUtils {

  private JFXUtils() {}

  public static MenuItem getMenuButton(String text, EventHandler<ActionEvent> e) {
    MenuItem item = new MenuItem(text);
    item.setMnemonicParsing(true);
    item.setOnAction(e);
    return item;
  }

  public static Alert getDialog(AlertType type, String title, String headerText, String contentText) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(headerText);
    alert.setContentText(contentText);
    return alert;
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

  public static Alert getDialogWithContent(Node content, String title, String headerText, String contentText) {
    Alert alert = getDialog(AlertType.INFORMATION, contentText, contentText, contentText);
    alert.getDialogPane().setContent(content);
    return alert;
  }
}
