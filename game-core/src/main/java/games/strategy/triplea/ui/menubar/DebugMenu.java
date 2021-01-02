package games.strategy.triplea.ui.menubar;

import games.strategy.engine.player.Player;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.menubar.debug.AiPlayerDebugAction;
import games.strategy.triplea.ui.menubar.debug.AiPlayerDebugOption;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import org.triplea.swing.SwingAction;

public final class DebugMenu extends JMenu {
  private static final long serialVersionUID = -4876915214715298132L;

  /** Maps the debug menu title to the function that will create the sub menu items */
  private static final Map<String, Function<TripleAFrame, Collection<JMenuItem>>>
      menuItemsAndFactories = new TreeMap<>();

  private static final SortedMap<String, List<AiPlayerDebugOption>> debugOptions = new TreeMap<>();

  private final TripleAFrame frame;

  DebugMenu(final TripleAFrame frame) {
    super("Debug");
    this.frame = frame;

    setMnemonic(KeyEvent.VK_D);

    menuItemsAndFactories.forEach(
        (name, factory) -> {
          final JMenu playerDebugMenu = new JMenu(name);
          add(playerDebugMenu);
          factory.apply(frame).forEach(playerDebugMenu::add);
        });

    debugOptions.forEach(
        (name, options) -> {
          final JMenu playerDebugMenu = new JMenu(name);
          add(playerDebugMenu);
          renderDebugOption(options).forEach(playerDebugMenu::add);
        });
  }

  private Collection<JMenuItem> renderDebugOption(final Collection<AiPlayerDebugOption> options) {
    final Map<String, Map<JMenuItem, AiPlayerDebugAction>> radioButtonDeselectGroups =
        new HashMap<>();
    final Map<String, ButtonGroup> radioButtonGroups = new HashMap<>();

    return options.stream()
        .map(
            option -> {
              if (!option.getSubActions().isEmpty()) {
                return renderSubMenuDebugOption(option);
              } else {
                final AiPlayerDebugAction debugAction = buildDebugAction();
                final JMenuItem menuItem = renderItemDebugOption(option, debugAction);

                if (option.getActionType() == AiPlayerDebugOption.ActionType.ON_OFF) {
                  menuItem.addItemListener(
                      e -> {
                        if (e.getStateChange() == ItemEvent.DESELECTED) {
                          debugAction.deselect();
                        }
                      });
                } else if (option.getActionType()
                    == AiPlayerDebugOption.ActionType.ON_OFF_EXCLUSIVE) {
                  radioButtonDeselectGroups
                      .computeIfAbsent(option.getExclusiveGroup(), k -> new HashMap<>())
                      .put(menuItem, debugAction);
                  radioButtonGroups
                      .computeIfAbsent(option.getExclusiveGroup(), k -> new ButtonGroup())
                      .add(menuItem);
                  // when a radio button is clicked, go through all of the other radio buttons
                  // in the group and deselect them
                  menuItem.addItemListener(
                      e -> {
                        radioButtonDeselectGroups
                            .get(option.getExclusiveGroup())
                            .entrySet()
                            .stream()
                            .filter(entry -> !entry.getKey().equals(e.getSource()))
                            .forEach(entry -> entry.getValue().deselect());
                      });
                }
                // ActionType.NORMAL doesn't use deselect so it doesn't need extra logic

                return menuItem;
              }
            })
        .collect(Collectors.toList());
  }

  private JMenu renderSubMenuDebugOption(final AiPlayerDebugOption option) {
    final JMenu subMenu = new JMenu(option.getTitle());
    renderDebugOption(option.getSubActions()).forEach(subMenu::add);
    return subMenu;
  }

  private AiPlayerDebugAction buildDebugAction() {
    return new AiPlayerDebugAction(frame.getMapPanel(), frame.getTerritoryDetails());
  }

  private JMenuItem renderItemDebugOption(
      final AiPlayerDebugOption option, final AiPlayerDebugAction action) {
    final Action swingAction =
        SwingAction.of(option.getTitle(), (evt) -> option.getActionListener().accept(action));
    switch (option.getActionType()) {
      case NORMAL:
      default:
        return new JMenuItem(swingAction);
      case ON_OFF:
        return new JCheckBoxMenuItem(swingAction);
      case ON_OFF_EXCLUSIVE:
        return new JRadioButtonMenuItem(swingAction);
    }
  }

  public static void registerMenuCallback(
      final String name, final Function<TripleAFrame, Collection<JMenuItem>> factory) {
    menuItemsAndFactories.put(name, factory);
  }

  public static void registerDebugOptions(
      final Player player, final List<AiPlayerDebugOption> options) {
    debugOptions.put(player.getName(), options);
  }
}
