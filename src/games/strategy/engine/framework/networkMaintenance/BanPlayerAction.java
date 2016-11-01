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

public class BanPlayerAction extends MenuItem {

  public BanPlayerAction(final IServerMessenger messenger) {
    super("Ban Player From Game");
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
      dialog.setTitle("Ban Player");
      dialog.setContentText("Select Player to ban:");

      dialog.showAndWait().ifPresent(name -> {
        for (final INode node : messenger.getNodes()) {
          if (node.getName().equals(name)) {
            final String realName = node.getName().split(" ")[0];
            final String ip = node.getAddress().getHostAddress();
            final String mac = messenger.getPlayerMac(node.getName());
            messenger.NotifyUsernameMiniBanningOfPlayer(realName, null);
            messenger.NotifyIPMiniBanningOfPlayer(ip, null);
            messenger.NotifyMacMiniBanningOfPlayer(mac, null);
            messenger.removeConnection(node);
            return;
          }
        }
      });
    });
  }
}
