package games.strategy.triplea.odds.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.ui.TripleAFrame;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import org.triplea.java.collections.IntegerMap;
import org.triplea.swing.key.binding.KeyCode;
import org.triplea.swing.key.binding.SwingKeyBinding;

/**
 * A dialog that allows the user to set up an arbitrary battle and calculate the attacker's odds of
 * successfully winning the battle. Also known as the Battle Calculator.
 */
public class BattleCalculatorDialog extends JDialog {
  private static final long serialVersionUID = -7625420355087851930L;
  private static Point lastPosition;
  private static final List<BattleCalculatorDialog> instances = new ArrayList<>();
  private final BattleCalculatorPanel panel;

  BattleCalculatorDialog(final BattleCalculatorPanel panel, final JFrame parent) {
    super(parent, "Battle Calculator");
    this.panel = panel;
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(panel, BorderLayout.CENTER);
    pack();
  }

  /**
   * Shows the Odds Calculator dialog and initializes it using the current state of the specified
   * territory.
   */
  public static void show(
      final TripleAFrame taFrame, @Nullable final Territory territory, final GameData data) {
    // Note: The data param may not be the same as taFrame.getGame().getData() as GameData gets
    // cloned when showing history with a different History instance that doesn't correspond to
    // what's shown.
    final BattleCalculatorPanel panel =
        new BattleCalculatorPanel(data, taFrame.getUiContext(), territory);
    final BattleCalculatorDialog dialog = new BattleCalculatorDialog(panel, taFrame);
    dialog.pack();

    dialog.addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowActivated(final WindowEvent e) {
            instances.remove(dialog);
            instances.add(dialog);
          }

          @Override
          public void windowClosed(final WindowEvent e) {
            if (taFrame.getUiContext() != null && !taFrame.getUiContext().isShutDown()) {
              taFrame.getUiContext().removeShutdownWindow(dialog);
            }
          }
        });

    dialog.addComponentListener(
        new ComponentAdapter() {
          @Override
          public void componentResized(final ComponentEvent e) {
            final Dimension size = dialog.getSize();
            size.width = Math.min(size.width, taFrame.getWidth() - 50);
            size.height = Math.min(size.height, taFrame.getHeight() - 50);
            dialog.setSize(size);
          }
        });

    taFrame.getTerritoryDetailPanel().addBattleCalculatorKeyBindings(dialog);
    // close when hitting the escape key
    SwingKeyBinding.addKeyBinding(
        dialog,
        KeyCode.ESCAPE,
        () -> {
          dialog.setVisible(false);
          dialog.dispose();
        });
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    if (lastPosition == null) {
      dialog.setLocationRelativeTo(taFrame);
    } else {
      dialog.setLocation(lastPosition);
    }
    dialog.setLocationRelativeTo(null); // center on screen
    dialog.setVisible(true);
    // On some desktop environments, there is an odd behaviour: The new battle calculator dialog is
    // not put at the front if the last user-interaction was to move the window of an already
    // existing battle calculator dialog.  Oddly enough, calling toFront() directly here (before or
    // after setVisible(true)) has no effect either, but delaying the call to the end of the queue
    // of the Event Dispatch Thread solves the issue (though you can see the new dialog in the
    // background for a blink of an eye).
    // Tested with Cinnamon Desktop 4.8.5.
    SwingUtilities.invokeLater(dialog::toFront);
    taFrame.getUiContext().addShutdownWindow(dialog);
  }

  private static void adjustBattleCalculatorPanel(
      final Territory t, Consumer<BattleCalculatorPanel> battleCalculatorPanelConsumer) {
    if (instances.isEmpty() || t == null) {
      return;
    }

    final BattleCalculatorDialog currentDialog = instances.get(instances.size() - 1);
    battleCalculatorPanelConsumer.accept(currentDialog.panel);
    currentDialog.pack();
  }

  public static void addAttackers(final Territory t) {
    adjustBattleCalculatorPanel(
        t,
        panel -> {
          // if there are no units set on the battle calculator panel yet,
          // then we'll determine the attacker to be the defender's enemy player
          // with the most units in the selected territory.
          if (panel.hasAttackingUnits()) {
            panel.addAttackingUnits(t.getMatches(Matches.unitIsOwnedBy(panel.getAttacker())));
          } else {
            // Find possible attacker (enemy) units for the current defender.
            final List<Unit> units =
                t.getUnitCollection().stream()
                    .filter(Matches.enemyUnit(panel.getDefender()))
                    .toList();

            if (!units.isEmpty()) {
              // Count how many units each one has and find the max to update the panel
              final IntegerMap<GamePlayer> unitCountMap = new IntegerMap<>(units, Unit::getOwner);
              final GamePlayer newAttacker = unitCountMap.maxKey();
              final List<Unit> attackingUnits =
                  units.stream().filter(Matches.unitIsOwnedBy(newAttacker)).toList();
              panel.setAttackerWithUnits(newAttacker, attackingUnits);
            }
          }
        });
  }

  public static void addDefenders(final Territory t) {
    adjustBattleCalculatorPanel(
        t,
        panel -> {
          // if there are no units added to the dialog, then we'll automatically
          // select the defending side to match any unit in the current territory
          if (!panel.hasAttackingUnits() && !panel.hasDefendingUnits()) {
            final Optional<GamePlayer> defender =
                t.getUnitCollection().stream().map(Unit::getOwner).findAny();
            defender.ifPresent(
                gamePlayer ->
                    panel.setDefenderWithUnits(
                        gamePlayer, t.getMatches(Matches.alliedUnit(gamePlayer))));
          } else {
            panel.addDefendingUnits(t.getMatches(Matches.alliedUnit(panel.getDefender())));
          }
        });
  }

  @Override
  public void dispose() {
    disposeInstance(this);
    super.dispose();
  }

  private static synchronized void disposeInstance(BattleCalculatorDialog currentDialog) {
    instances.remove(currentDialog);
    lastPosition = new Point(currentDialog.getLocation());
  }
}
