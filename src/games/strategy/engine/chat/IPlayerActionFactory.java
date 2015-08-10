package games.strategy.engine.chat;

import java.util.Collections;
import java.util.List;

import javax.swing.Action;

import games.strategy.net.INode;

public interface IPlayerActionFactory {
  public static final IPlayerActionFactory NULL_FACTORY = new IPlayerActionFactory() {
    @Override
    public List<Action> mouseOnPlayer(final INode clickedOn) {
      return Collections.emptyList();
    }
  };

  /**
   * The mouse has been clicked on a player, create a list of actions to be displayed
   */
  public List<Action> mouseOnPlayer(INode clickedOn);
}
