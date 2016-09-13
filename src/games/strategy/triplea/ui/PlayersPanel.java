package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.PlayerManager;
import games.strategy.engine.framework.IGame;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Panel to show who is playing which players
 */
public class PlayersPanel extends VBox {
  public PlayersPanel(final PlayerManager players, final GameData data) {
    for (final String player : players.getPlayers()) {
      final PlayerID playerID = data.getPlayerList().getPlayerID(player);
      if (playerID.isAI()) {
        getChildren().add(new Label(playerID.getWhoAmI().split(":")[1] + " is " + playerID.getName()));
      } else {
        getChildren().add(new Label(players.getNode(player).getName() + " is " + playerID.getName()));
      }
    }
  }

  public static void showPlayers(final IGame game) {
    final PlayersPanel panel = new PlayersPanel(game.getPlayerManager(), game.getData());
    Alert info = new Alert(AlertType.INFORMATION);
    info.setTitle("Players");
    info.getDialogPane().getChildren().setAll(panel);
    info.show();
  }
}
