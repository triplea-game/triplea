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

public class MutePlayerAction extends MenuItem {

  public MutePlayerAction(final IServerMessenger messenger) {
    super("Mute Player's Chatting");
    setOnAction(e -> {
      final List<String> combo = new ArrayList<>();
      for (final INode node : new TreeSet<>(messenger.getNodes())) {
        if (!node.equals(messenger.getLocalNode())) {
          combo.add(node.getName());
        }
      }
      if (combo.size() == 0) {
        new Alert(AlertType.ERROR, "No Remote Players").show();
        return;
      }
      ChoiceDialog<String> dialog = new ChoiceDialog<>(combo.get(0), combo);
      dialog.setTitle("Mute Player");
      dialog.setContentText("Select Player to mute:");

      dialog.showAndWait().ifPresent(name -> {
        for (final INode node : messenger.getNodes()) {
          if (node.getName().equals(name)) {
            final String realName = node.getName().split(" ")[0];
            final String ip = node.getAddress().getHostAddress();
            final String mac = messenger.getPlayerMac(node.getName());
            messenger.NotifyUsernameMutingOfPlayer(realName, null);
            messenger.NotifyIPMutingOfPlayer(ip, null);
            messenger.NotifyMacMutingOfPlayer(mac, null);
            return;
          }
        }
      });
    });
  }
}
