package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.triplea.swing.SwingAction;
import org.triplea.swing.WrapLayout;

/**
 * A UI component used to display a dice roll. One image is displayed per die in a horizontal
 * layout.
 */
public class DicePanel extends JPanel {
  private static final long serialVersionUID = -7544999867518263506L;
  private final UiContext uiContext;
  private final GameData data;

  public DicePanel(final UiContext uiContext, final GameData data) {
    this.uiContext = uiContext;
    this.data = data;
    setLayout(new GridBagLayout());
    setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
  }

  void setDiceRollForBombing(final List<Die> dice, final int cost) {
    removeAll();
    add(create(dice));
    addBottomLabel(new JLabel("Cost:" + cost));
  }

  /** Sets the dice roll to display. */
  public void setDiceRoll(final DiceRoll diceRoll) {
    SwingAction.invokeNowOrLater(
        () -> {
          removeAll();
          for (int i = 1; i <= data.getDiceSides(); i++) {
            final List<Die> dice = diceRoll.getRolls(i);
            if (dice.isEmpty()) {
              continue;
            }
            add(new JLabel("Rolled at " + i + ":"));
            add(create(diceRoll.getRolls(i)));
          }
          addBottomLabel(new JLabel("Total hits:" + diceRoll.getHits(), SwingConstants.LEFT));
        });
  }

  private void add(final JComponent component) {
    final GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1;
    constraints.gridx = 0;
    add(component, constraints);
  }

  private void addBottomLabel(final JLabel label) {
    final GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.BOTH;
    constraints.weightx = 1;
    constraints.weighty = 1;
    constraints.gridx = 0;
    label.setVerticalAlignment(SwingConstants.BOTTOM);
    add(label, constraints);
  }

  private JComponent create(final List<Die> dice) {
    final JPanel dicePanel = new JPanel();
    dicePanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
    dicePanel.setLayout(new WrapLayout(WrapLayout.LEFT, 2, 2));
    for (final Die die : dice) {
      final int roll = die.getValue() + 1;
      dicePanel.add(new JLabel(uiContext.getDiceImageFactory().getDieIcon(roll, die.getType())));
    }
    return dicePanel;
  }
}
