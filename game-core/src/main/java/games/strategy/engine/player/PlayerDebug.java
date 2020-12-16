package games.strategy.engine.player;

import games.strategy.triplea.ui.TripleAFrame;
import java.util.function.Function;
import javax.swing.Action;
import javax.swing.JMenuItem;

public interface PlayerDebug {
  void addDebugMenuItems(TripleAFrame frame, Function<Action, JMenuItem> addMenuItem);
}
