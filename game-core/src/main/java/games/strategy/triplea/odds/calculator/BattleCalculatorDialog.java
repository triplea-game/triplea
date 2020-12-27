package games.strategy.triplea.odds.calculator;

import games.strategy.engine.data.Territory;
import games.strategy.engine.history.History;
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
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
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
  public static void show(final TripleAFrame taFrame, final Territory t, final History history) {
    // Note: The history param may not be the same as taFrame.getGame().getData().getHistory() as
    // GameData
    // gets cloned when showing history with a different History instance that doesn't correspond to
    // what's
    // shown.
    final BattleCalculatorPanel panel =
        new BattleCalculatorPanel(taFrame.getGame().getData(), history, taFrame.getUiContext(), t);
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
    dialog.setVisible(true);
    taFrame.getUiContext().addShutdownWindow(dialog);
  }

  public static void addAttackers(final Territory t) {
    if (instances.isEmpty()) {
      return;
    }
    final BattleCalculatorPanel currentPanel = instances.get(instances.size() - 1).panel;
    currentPanel.addAttackingUnits(
        t.getUnitCollection().getMatches(Matches.unitIsOwnedBy(currentPanel.getAttacker())));
  }

  public static void addDefenders(final Territory t) {
    if (instances.isEmpty() || t == null) {
      return;
    }
    final BattleCalculatorDialog currentDialog = instances.get(instances.size() - 1);
    currentDialog.panel.addDefendingUnits(
        t.getUnitCollection()
            .getMatches(
                Matches.alliedUnit(
                    currentDialog.panel.getDefender(), t.getData().getRelationshipTracker())));
    currentDialog.pack();
  }

  @Override
  public void dispose() {
    instances.remove(this);
    lastPosition = new Point(getLocation());
    super.dispose();
  }

  @Override
  public void setVisible(final boolean vis) {
    super.setVisible(vis);
    panel.selectCalculateButton();
  }
}
