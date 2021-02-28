package games.strategy.engine.chat;

import java.util.List;
import javax.swing.Action;
import org.triplea.domain.data.ChatParticipant;

/** Factory for creating actions in response to a player (node) event. */
@FunctionalInterface
public interface IPlayerActionFactory {

  /** The mouse has been clicked on a player, create a list of actions to be displayed. */
  List<Action> mouseOnPlayer(ChatParticipant clickedOn);
}
