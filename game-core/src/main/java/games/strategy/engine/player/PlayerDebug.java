package games.strategy.engine.player;

import games.strategy.triplea.ui.TripleAFrame;
import java.util.Collection;
import javax.swing.JMenuItem;

public interface PlayerDebug {
  Collection<JMenuItem> addDebugMenuItems(TripleAFrame frame);
}
