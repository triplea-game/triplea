package games.strategy.triplea.ui.menubar;

import games.strategy.engine.player.Player;
import games.strategy.triplea.ai.pro.AbstractProAi;
import games.strategy.triplea.ai.pro.logging.ProLogUi;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.menubar.debug.AiPlayerDebugAction;
import games.strategy.triplea.ui.menubar.debug.AiPlayerDebugOption;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.triplea.ai.flowfield.FlowFieldAi;
import org.triplea.ai.flowfield.odds.LanchesterDebugAction;
import org.triplea.swing.JMenuBuilder;
import org.triplea.swing.SwingAction;
import org.triplea.swing.key.binding.KeyCode;

@UtilityClass
public final class DebugMenu {

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @Getter
  public enum Mnemonic {
    AI_NORMAL(KeyCode.A),
    AI_HARD(KeyCode.H);

    private final KeyCode mnemonicCode;

    public int getValue() {
      return mnemonicCode.getInputEventCode();
    }
  }

  public static JMenu get(final TripleAFrame frame) {
    final JMenu debugMenu =
        new JMenuBuilder("Debug", TripleAMenuBar.Mnemonic.DEBUG.getMnemonicCode()).build();

    final List<JMenu> subMenus = getSubMenus(frame);
    if (subMenus.isEmpty()) {
      debugMenu.setVisible(false);
    } else {
      subMenus.stream().sorted(Comparator.comparing(JMenu::getText)).forEach(debugMenu::add);
    }

    return debugMenu;
  }

  private static List<JMenu> getSubMenus(TripleAFrame frame) {
    final List<JMenu> subMenus = new ArrayList<>();
    boolean addedProAiOption = false;
    for (Player player : frame.getLocalPlayers().getLocalPlayers()) {
      if (player instanceof FlowFieldAi ai) {
        JMenuBuilder subMenuBuilder =
            new JMenuBuilder(ai.getName(), Mnemonic.AI_NORMAL.getMnemonicCode());
        renderDebugOption(frame, LanchesterDebugAction.buildDebugOptions(ai))
            .forEach(subMenuBuilder::addMenuItem);
        subMenus.add(subMenuBuilder.build());
      } else if (!addedProAiOption && player instanceof AbstractProAi) {
        JMenuBuilder menuBuilder = new JMenuBuilder("Hard AI", Mnemonic.AI_HARD.getMnemonicCode());
        renderDebugOption(frame, ProLogUi.buildDebugOptions(frame))
            .forEach(menuBuilder::addMenuItem);
        subMenus.add(menuBuilder.build());
        addedProAiOption = true;
      }
    }
    return subMenus;
  }

  private static JMenu renderSubMenuDebugOption(
      final TripleAFrame frame, final AiPlayerDebugOption option) {
    final JMenuBuilder subMenuBuilder = new JMenuBuilder(option.getTitle(), KeyCode.S);
    renderDebugOption(frame, option.getSubOptions()).forEach(subMenuBuilder::addMenuItem);
    return subMenuBuilder.build();
  }

  private static JMenuItem renderItemDebugOption(
      final AiPlayerDebugOption option, final AiPlayerDebugAction action) {
    final Action swingAction =
        SwingAction.of(option.getTitle(), evt -> option.getActionListener().accept(action));
    switch (option.getOptionType()) {
      case ON_OFF:
        return new JCheckBoxMenuItem(swingAction);
      case ON_OFF_EXCLUSIVE:
        return new JRadioButtonMenuItem(swingAction);
      case NORMAL:
      default:
        return new JMenuItem(swingAction);
    }
  }

  private static Collection<JMenuItem> renderDebugOption(
      final TripleAFrame frame, Collection<AiPlayerDebugOption> options) {
    // keep track of all the radio button menu items and their actions so that when one of the
    // radio buttons is selected, the other radio buttons can be deselected
    final Map<String, Map<JMenuItem, AiPlayerDebugAction>> radioButtonDeselectGroups =
        new HashMap<>();
    // each group of radio buttons needs a ButtonGroup so selecting one will deselect the others
    final Map<String, ButtonGroup> radioButtonGroups = new HashMap<>();

    // this stream converts the AiPlayerDebugOption into JMenuItems
    // if the option has sub options, then create a JMenu and recurse to create the sub menu items
    // otherwise, if the option has the actionType == NORMAL, then create a JMenuItem
    // if the option has the actionType == ON_OFF, create a JCheckBoxMenuItem and add a listener
    // to listen for deselection
    // if the option has the actionType == ON_OFF_EXCLUSIVE, create a JRadioButtonMenuItem and
    // add a listener for deselection. Also, add the JRadioButtonMenuItem to a ButtonGroup so that
    // each group of radio buttons only allow one to be selected at a time.
    return options.stream()
        .map(
            option -> {
              if (!option.getSubOptions().isEmpty()) {
                return renderSubMenuDebugOption(frame, option);
              } else {
                final AiPlayerDebugAction debugAction =
                    new AiPlayerDebugAction(
                        frame.getMapPanel(), frame.getAdditionalTerritoryDetails());
                final JMenuItem menuItem = renderItemDebugOption(option, debugAction);

                final int mnemonic = option.getMnemonic();
                menuItem.setMnemonic(mnemonic);
                if (option.getOptionType() == AiPlayerDebugOption.OptionType.ON_OFF) {
                  menuItem.addItemListener(
                      e -> {
                        if (e.getStateChange() == ItemEvent.DESELECTED) {
                          debugAction.deselect();
                        }
                      });
                } else if (option.getOptionType()
                    == AiPlayerDebugOption.OptionType.ON_OFF_EXCLUSIVE) {
                  radioButtonDeselectGroups
                      .computeIfAbsent(option.getExclusiveGroup(), k -> new HashMap<>())
                      .put(menuItem, debugAction);
                  radioButtonGroups
                      .computeIfAbsent(option.getExclusiveGroup(), k -> new ButtonGroup())
                      .add(menuItem);
                  // when a radio button is clicked, go through all of the other radio buttons
                  // in the group and deselect them
                  menuItem.addItemListener(
                      e ->
                          radioButtonDeselectGroups
                              .get(option.getExclusiveGroup())
                              .entrySet()
                              .stream()
                              .filter(entry -> !entry.getKey().equals(e.getSource()))
                              .forEach(entry -> entry.getValue().deselect()));
                }
                // ActionType.NORMAL doesn't use deselect so it doesn't need extra logic

                return menuItem;
              }
            })
        .collect(Collectors.toList());
  }
}
