package games.strategy.engine.chat;

import java.util.Collections;
import java.util.List;

import games.strategy.net.INode;
import javafx.scene.Node;

public interface IPlayerActionFactory {
  static IPlayerActionFactory NULL_FACTORY = clickedOn -> Collections.emptyList();

  /**
   * The mouse has been clicked on a player, create a list of actions to be displayed
   */
  List<Node> mouseOnPlayer(INode clickedOn);
}
