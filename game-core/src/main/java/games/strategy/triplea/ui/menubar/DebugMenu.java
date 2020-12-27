package games.strategy.triplea.ui.menubar;

import games.strategy.triplea.ui.TripleAFrame;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

public final class DebugMenu extends JMenu {
  private static final long serialVersionUID = -4876915214715298132L;

  /** Maps the debug menu title to the function that will create the sub menu items */
  private static final Map<String, Function<TripleAFrame, Collection<JMenuItem>>>
      menuItemsAndFactories = new TreeMap<>();

  DebugMenu(final TripleAFrame frame) {
    super("Debug");

    setMnemonic(KeyEvent.VK_D);

    menuItemsAndFactories.forEach(
        (name, factory) -> {
          final JMenu playerDebugMenu = new JMenu(name);
          add(playerDebugMenu);
          factory.apply(frame).forEach(playerDebugMenu::add);
        });
  }

  public static void registerMenuCallback(
      final String name, final Function<TripleAFrame, Collection<JMenuItem>> factory) {
    menuItemsAndFactories.put(name, factory);
  }

  public static void unregisterMenuCallback(final String name) {
    menuItemsAndFactories.remove(name);
  }
}
