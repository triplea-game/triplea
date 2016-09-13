package games.strategy.engine.framework.networkMaintenance;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.MenuItem;

public class BootPlayerAction extends MenuItem {

  public BootPlayerAction(final IServerMessenger messenger) {
    super("Remove Player");
    setOnAction(e -> {
      final List<String> players = new ArrayList<>();
      for (final INode node : new TreeSet<>(messenger.getNodes())) {
        if (!node.equals(messenger.getLocalNode())) {
          players.add(node.getName());
        }
      }
      if (players.size() == 0) {
        new Alert(AlertType.ERROR, "No Remote Players").show();
        return;
      }
      ChoiceDialog<String> dialog = new ChoiceDialog<>(players.get(0), players);
      dialog.setTitle("Remove Player");
      dialog.setContentText("Select Player to remove:");

      dialog.showAndWait().ifPresent(name -> {
        for (final INode node : messenger.getNodes()) {
          if (node.getName().equals(name)) {
            messenger.removeConnection(node);
          }
        }
      });
    });
  }
}
