package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.lookandfeel.LookAndFeel;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.WrapLayout;

/**
 * A UI component used to display a roll of dice. One image is displayed per die in a horizontal
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
    setBorder(BorderFactory.createEmptyBorder(8, 16, 0, 0));
  }

  void setDiceRollForBombing(final List<Die> dice, final int cost) {
    removeAll();
    add(new JLabel("Cost: " + cost, SwingConstants.LEFT));
    add(create(dice));
    addBottomSpacing();
  }

  /** Sets the dice roll to display. */
  public void setDiceRoll(final DiceRoll diceRoll) {
    removeAll();
    final String hitsString = colorizeHitString(diceRoll.getHits());
    add(
        new JLabel(
            "<html><b><font style='font-size:120%'>Total hits: " + hitsString,
            SwingConstants.LEFT));
    add(new JSeparator());

    for (int i = 1; i <= data.getDiceSides(); i++) {
      final List<Die> dice = diceRoll.getRolls(i);
      if (dice.isEmpty()) {
        continue;
      }
      add(makeDiceRolledLabel(dice, i));
      add(create(dice));
    }
    addBottomSpacing();
    SwingComponents.redraw(this);
  }

  private JLabel makeDiceRolledLabel(final List<Die> dice, final int value) {
    final long hits = dice.stream().map(Die::getType).filter(Die.DieType.HIT::equals).count();
    final String countString = dice.size() == 1 ? "1 die" : dice.size() + " dice";
    final String hitsString = colorizeHitString(hits == 1 ? "1 hit" : hits + " hits");
    return new JLabel("<html><b>Rolled " + countString + " at " + value + " (" + hitsString + "):");
  }

  private static String colorizeHitString(final Object hitsString) {
    return "<font color='" + LookAndFeel.getLookAndFeelColorRed() + "'>" + hitsString + "</font>";
  }

  private void add(final JComponent component) {
    final GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1;
    constraints.gridx = 0;
    add(component, constraints);
  }

  private void addBottomSpacing() {
    final GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.BOTH;
    constraints.weightx = 1;
    constraints.weighty = 1;
    constraints.gridx = 0;
    add(Box.createVerticalGlue(), constraints);
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
